(ns core.logging
  (:require [clojure.java.io :as io]
            [taoensso.timbre :as timbre]
            [cheshire.core :as json]))

(defn- json-output-fn
  "Converts timbre's log-data map into a single-line JSON string."
  [{:keys [instant level ?ns-str msg_ args data]}]
  (json/generate-string
   (cond-> {:timestamp (str instant)
            :level     (name level)
            :ns        ?ns-str
            :msg       (force msg_)}
     (seq args) (assoc :args (vec args))
     (seq data) (assoc :data data))))

(defn init-logging!
  "Structured JSON logging to stdout and a file. Each log line is one JSON
  object, e.g.
    {\"timestamp\":\"2025-…\",\"level\":\"info\",\"ns\":\"core.queue\",
     \"msg\":\"job completed\",\"args\":[\":job-id\",\"…\",\":duration-ms\",42]}"
  []
  (io/make-parents "logs/fishvision.log")
  (timbre/set-config!
   {:level     :info
    :output-fn json-output-fn
    :appenders {:println (timbre/println-appender {})
                :spit    (timbre/spit-appender {:fname "logs/fishvision.log"})}}))
