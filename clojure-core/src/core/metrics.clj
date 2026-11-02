(ns core.metrics
  "Tiny dependency-free metrics registry.  Good enough for a single-node
  deployment; swap for iapetos/Prometheus if you need scraping and histograms."
  (:require [taoensso.timbre :as log])
  (:import [java.util.concurrent ConcurrentHashMap]
           [java.util.concurrent.atomic AtomicLong LongAdder]))

(defonce ^:private counters (ConcurrentHashMap.))
(defonce ^:private timers   (ConcurrentHashMap.))   ; name -> [count, total-ms]

(defn- counter ^AtomicLong [k]
  (.computeIfAbsent counters k (reify java.util.function.Function
                                 (apply [_ _] (AtomicLong. 0)))))

(defn inc-counter!
  ([k] (inc-counter! k 1))
  ([k n] (.addAndGet (counter k) (long n))))

(defn observe-timer!
  "Records one observation for timer `k` (milliseconds)."
  [k ms]
  (let [^longs cell (.computeIfAbsent timers k
                                      (reify java.util.function.Function
                                        (apply [_ _] (long-array [0 0]))))]
    (locking cell
      (aset cell 0 (inc (aget cell 0)))
      (aset cell 1 (+ (aget cell 1) (long ms))))))

(defn snapshot
  "Returns a plain map suitable for JSON serialisation."
  []
  {:counters (into {} (for [[k v] counters] [k (.get ^AtomicLong v)]))
   :timers   (into {}
                   (for [[k ^longs cell] timers]
                     (let [n (aget cell 0) total (aget cell 1)]
                       [k {:count n
                           :total-ms total
                           :avg-ms (if (pos? n) (double (/ total n)) 0.0)}])))})

(defn start-reporter!
  "Logs a metrics snapshot every `interval-ms` (default 60 s) on a daemon
  thread.  Call once from `start-server!`."
  ([] (start-reporter! 60000))
  ([interval-ms]
   (doto (Thread.
          (fn []
            (loop []
              (Thread/sleep (long interval-ms))
              (log/info "metrics snapshot" (snapshot))
              (recur))))
     (.setDaemon true)
     (.setName "metrics-reporter")
     (.start))))
