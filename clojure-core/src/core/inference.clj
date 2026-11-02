(ns core.inference
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [taoensso.timbre :as log]
            [uncomplicate.commons.core :refer [release]]
            [uncomplicate.neanderthal.core :refer [transfer!]]
            [uncomplicate.neanderthal.native]
            [uncomplicate.diamond.tensor :refer [tensor]]
            [uncomplicate.diamond.onnxrt :refer [onnx]]
            [uncomplicate.diamond.internal.dnnl.factory :refer [dnnl-factory]]))

(def class-labels [:fresh :highly-fresh :not-fresh])

;; 5 * 224 * 224
(def ^:const expected-input-len 250880)

;; Cached JVM class objects for primitive-array type checks
(def ^:private double-array-class (Class/forName "[D"))
(def ^:private float-array-class  (Class/forName "[F"))

;; ---------------------------------------------------------------------------
;; DNNL factory — one per JVM
;; ---------------------------------------------------------------------------

(defonce ^:private factory
  (delay (dnnl-factory)))

;; ---------------------------------------------------------------------------
;; ONNX blueprint cache
;; ---------------------------------------------------------------------------

(defonce ^:private blueprint-cache (atom {}))

(defn- onnx-ep-options
  "Default: no EP explicitly registered (`:ep []` = default CPU EP).
   Override with ONNX_EP env var only if verified on your machine."
  []
  (if-let [ep (System/getenv "ONNX_EP")]
    (let [kw (keyword (str/replace ep #"^:" ""))]
      (log/warn "ONNX_EP override in effect:" kw)
      {:ep [kw]})
    {:ep []}))

(defn- build-blueprint
  [model-path]
  (let [abs-path (.getAbsolutePath (io/file model-path))]
    (when-not (.exists (io/file abs-path))
      (throw (ex-info (str "ONNX model not found: " abs-path)
                      {:model-path abs-path})))
    (or (get @blueprint-cache abs-path)
        (let [opts (onnx-ep-options)]
          (log/info "loading ONNX model" abs-path "with options" opts)
          (let [bp (onnx @factory abs-path opts)]
            (log/info "ONNX model loaded" abs-path)
            (swap! blueprint-cache assoc abs-path bp)
            bp)))))

(defn- validate-input [input-data]
  (when-not (instance? (class (float-array 0)) input-data)
    (throw (ex-info "input-data must be a float-array"
                    {:type (type input-data)})))
  (let [n (alength ^"[F" input-data)]
    (when-not (= n expected-input-len)
      (throw (ex-info (str "Expected " expected-input-len
                           " input floats, got " n)
                      {:expected expected-input-len :actual n})))))

;; ---------------------------------------------------------------------------
;; Logits inference
;; ---------------------------------------------------------------------------

(defn predict-array
  "Takes a flat float-array (from Rust gRPC GetTensor) and returns
   {:prediction kw :logits [f f f]}."
  [input-data model-path]
  (validate-input input-data)
  (let [input-tz (tensor @factory [1 5 224 224] :float :nchw)]
    (try
      (let [bp     (build-blueprint model-path)
            infer! (bp input-tz)]
        (transfer! input-data input-tz)
        (let [logits  (transfer! (infer!) (float-array 3))
              max-idx (apply max-key #(aget ^"[F" logits %) (range 3))]
          {:prediction (nth class-labels max-idx)
           :logits     (vec logits)}))
      (finally (release input-tz)))))

;; ---------------------------------------------------------------------------
;; Embedding inference (single-output model)
;; ---------------------------------------------------------------------------

(defn predict-embedding
  "Takes a flat float-array (from Rust gRPC GetTensor) and returns
   a 576-d embedding as a float-array. Model path should point to
   fish_model_embedding_only.onnx (single-output)."
  [input-data model-path]
  (validate-input input-data)
  (let [input-tz (tensor @factory [1 5 224 224] :float :nchw)]
    (try
      (let [bp     (build-blueprint model-path)
            infer! (bp input-tz)]
        (transfer! input-data input-tz)
        (transfer! (infer!) (float-array 576)))
      (finally (release input-tz)))))

;; ---------------------------------------------------------------------------
;; k-NN on frozen embeddings (k=1, cosine similarity)
;; Empirical MFED accuracy: 70.8% (k=1 best; k=3/5/9 were worse).
;; ---------------------------------------------------------------------------

(defn- ^double dot [^"[D" a ^"[D" b]
  (let [n (alength a)]
    (loop [i 0 acc 0.0]
      (if (< i n)
        (recur (unchecked-inc i) (+ acc (* (aget a i) (aget b i))))
        acc))))

(defn- ^"[D" to-doubles [v]
  (cond
    (instance? double-array-class v) v

    (instance? float-array-class v)
    (let [^"[F" fa v
          n  (alength fa)
          da (double-array n)]
      (dotimes [i n]
        (aset da i (double (aget fa i))))
      da)

    :else
    (let [n (count v)
          da (double-array n)]
      (doseq [[i x] (map-indexed vector v)]
        (aset da i (double x)))
      da)))

(defonce ^:private knn-reference
  (delay
    (if-let [f (io/resource "knn_reference.edn")]
      (let [raw (read-string (slurp f))]
        (log/info "loaded k-NN reference" :count (count raw))
        (mapv (fn [{:keys [label embedding]}]
                {:label label
                 :vec   (to-doubles embedding)})
              raw))
      (do (log/warn "knn_reference.edn not found — k-NN disabled")
          nil))))

(defn knn-classify
  "k-NN on L2-normalized embeddings (k=1). Returns
   {:prediction kw :confidence double :margin double :method :knn-1}.
   Confidence is top-1 cosine similarity in [0,1]."
  [embedding]
  (when-let [refs @knn-reference]
    (let [^"[D" q  (to-doubles embedding)
          qn       (Math/sqrt (dot q q))
          ^"[D" q-n (let [n (alength q)
                          a (double-array n)
                          d (max qn 1e-9)]
                      (dotimes [i n]
                        (aset a i (/ (aget q i) d)))
                      a)
          scored   (mapv (fn [ref]
                           (let [^"[D" v (:vec ref)]
                             {:label (:label ref)
                              :sim   (dot q-n v)}))
                         refs)
          sorted   (sort-by :sim > scored)
          top1     (first sorted)
          top2     (second sorted)]
      {:prediction (nth class-labels (:label top1))
       :confidence (double (:sim top1))
       :margin     (double (- (:sim top1) (:sim top2)))
       :method     :knn-1})))
