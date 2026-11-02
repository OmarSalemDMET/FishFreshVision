(ns core.api
  (:require [reitit.ring :as ring]
            [ring.adapter.jetty :as jetty]
            [muuntaja.core :as m]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [ring.middleware.multipart-params.temp-file :refer [temp-file-store]]
            [clojure.java.io :as io]
            [core.queue :as q]
            [core.db :as db]
            [core.dashboard :as dashboard]
            [core.metrics :as metrics]
            [core.logging :as logging]
            [taoensso.timbre :as log])
  (:import [java.util UUID] [java.io File]))

(defn upload-handler [{:keys [multipart-params]}]
  (if-let [file (get multipart-params "file")]
    (let [tmp-path  (cond
                      (instance? File file) (.getAbsolutePath ^File file)
                      (map? file)           (str (:tempfile file))
                      :else                 (str file))
          orig-name (if (map? file) (:filename file) (.getName (io/file tmp-path)))
          job-id    (str (UUID/randomUUID))

          upload-dir (io/file "uploads")
          _          (.mkdirs upload-dir)
          dest-file  (io/file upload-dir (str job-id "_" orig-name))
          _          (io/copy (io/file tmp-path) dest-file)
          final-path (.getAbsolutePath dest-file)

          model     "resources/fish_model_static.onnx"]

      (q/submit-job! job-id final-path model)
      (log/info "upload accepted" :job-id job-id :original-name orig-name)
      {:status 202 :body {:job-id job-id :message "Processing started"}})
    {:status 400 :body {:error "No file uploaded"}}))

(defn wrap-exception-handler [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (log/error e "unhandled API exception")
        {:status 500 :headers {"Content-Type" "application/json"} :body {:error "Internal Server Error"}}))))

(defn wrap-cors [handler]
  (fn [request]
    (let [response (if (= :options (:request-method request))
                     {:status 200 :headers {}}
                     (try
                       (handler request)
                       (catch Exception e
                         {:status 500 :headers {} :body {:error (ex-message e)}})))]
      (update response :headers merge {
        "Access-Control-Allow-Origin" "*"
        "Access-Control-Allow-Methods" "GET, POST, OPTIONS"
        "Access-Control-Allow-Headers" "Content-Type, Authorization"
      }))))

(def router
  (ring/ring-handler
   (ring/router
    [["/" {:get (fn [_] {:status 200 :headers {"Content-Type" "text/html"} :body "<h1>FishVision API</h1>"})}]

     ;; --- GUARANTEED IMAGE SERVING ---
     ["/uploads/:filename"
      {:get (fn [{{:keys [filename]} :path-params}]
              (let [f (io/file "uploads" filename)]
                (if (.exists f)
                  {:status 200
                   :headers {"Content-Type" (or (java.net.URLConnection/guessContentTypeFromName filename) "application/octet-stream")}
                   :body (io/input-stream f)}
                  {:status 404 :body {:error "Image not found"}})))}]

     ;; --- JSON API ---
     ["/api/jobs" {:get (fn [{{:keys [limit]} :query-params}]
                          {:status 200 :body (db/get-recent-jobs (if limit (Long/parseLong limit) 50))})}]
     ["/api/jobs/:id" {:get (fn [{{:keys [id]} :path-params}]
                              (if-let [job (db/get-job id)]
                                {:status 200 :body job}
                                {:status 404 :body {:error "Not found"}}))}]
     ["/api/metrics" {:get (fn [_] {:status 200 :body (metrics/snapshot)})}]

     ["/dashboard" {:get (fn [_] {:status 200 :headers {"Content-Type" "text/html"} :body (dashboard/render-page (db/get-recent-jobs 20))})}]
     ["/analyze" {:post {:handler upload-handler}}]]
    {:data {:muuntaja m/instance :middleware [muuntaja/format-middleware]}})
   (ring/create-default-handler {:not-found (fn [_] {:status 404 :body {:error "Not found"}})})))

(def app
  (-> router
      (wrap-multipart-params {:store (temp-file-store)})
      (wrap-cors)
      (wrap-exception-handler)))

(defn start-server! []
  (logging/init-logging!)
  (db/ensure-datasource!)
  (db/init-db!)
  (q/start-workers! 4)
  (metrics/start-reporter! 60000)
  (log/info "FishVision starting" :port 3000)
  (jetty/run-jetty app {:port 3000 :join? false}))
