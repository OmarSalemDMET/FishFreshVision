(ns core.queue
  (:require [clojure.core.async :as async]
            [clojure.string :as str]
            [core.engine-client :as engine]
            [core.inference :as inference]
            [core.agent :as agent]
            [core.db :as db]
            [core.metrics :as metrics]
            [taoensso.timbre :as log]))

(defonce job-queue (async/chan 100))

;; The logits model path is passed from the API. The embedding model is
;; derived by replacing "_static.onnx" with "_embedding_only.onnx", so any
;; model name dropped into resources/ needs its embedding counterpart too.
(defn- embedding-path-for [logits-path]
  (let [s (str logits-path)]
    (if (.endsWith s "_static.onnx")
      (str (subs s 0 (- (count s) (count "_static.onnx"))) "_embedding_only.onnx")
      (str/replace s #"\.onnx$" "_embedding_only.onnx"))))

(defn submit-job! [job-id image-path model-path]
  (db/insert-job! job-id image-path)
  (metrics/inc-counter! :jobs/submitted)
  (log/info "job submitted" :job-id job-id :path image-path)
  (async/>!! job-queue {:job-id job-id :path (str image-path) :model model-path}))

(defn process-job [job]
  (let [{:keys [job-id path model]} job
        started (System/nanoTime)
        emb-model (embedding-path-for model)]
    (db/set-job-processing! job-id)
    (log/info "processing job" :job-id job-id :path path)
    (try
      (let [tensor-start (System/nanoTime)
            tensor       (engine/get-tensor-array path)
            _            (metrics/observe-timer! :stage/tensor-ms
                                                 (/ (- (System/nanoTime) tensor-start) 1e6))

            cnn-start (System/nanoTime)
            cnn-out   (inference/predict-array tensor model)
            _         (metrics/observe-timer! :stage/cnn-ms
                                              (/ (- (System/nanoTime) cnn-start) 1e6))

            emb-start (System/nanoTime)
            emb       (inference/predict-embedding tensor emb-model)
            knn-out   (inference/knn-classify emb)
            _         (metrics/observe-timer! :stage/embed-knn-ms
                                              (/ (- (System/nanoTime) emb-start) 1e6))

            fft-start (System/nanoTime)
            analysis  (engine/analyze-image path)
            _         (metrics/observe-timer! :stage/fft-ms
                                              (/ (- (System/nanoTime) fft-start) 1e6))

            agent-out (agent/evaluate-decision
                       {:cnn      cnn-out
                        :knn      knn-out
                        :analysis analysis})

            elapsed-ms (/ (- (System/nanoTime) started) 1e6)]

        (db/save-job-result! job-id
                             {:cnn      cnn-out
                              :knn      knn-out
                              :agent    agent-out
                              :analysis analysis}
                             "done")
        (metrics/inc-counter! :jobs/completed)
        (metrics/observe-timer! :job/total-ms elapsed-ms)
        (log/info "job completed"
                  :job-id     job-id
                  :knn-pred   (some-> knn-out :prediction name)
                  :knn-conf   (some-> knn-out :confidence double)
                  :cnn-pred   (some-> cnn-out :prediction name)
                  :agree?     (get-in agent-out [:result :agree?])
                  :action     (some-> agent-out :result :action name)
                  :duration-ms elapsed-ms))

      (catch Exception e
        (metrics/inc-counter! :jobs/failed)
        (metrics/observe-timer! :job/total-ms (/ (- (System/nanoTime) started) 1e6))
        (log/error e "job failed" :job-id job-id)
        (db/save-job-error! job-id (ex-message e))))))

(defn start-workers! [n]
  (dotimes [_ n]
    (async/thread
      (loop []
        (when-let [j (async/<!! job-queue)]
          (process-job j)
          (recur))))))
