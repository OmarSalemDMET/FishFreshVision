(ns core.db
  (:require [next.jdbc :as jdbc]
            [next.jdbc.connection :as conn]
            [next.jdbc.result-set :as rs]
            [honey.sql :as sql]
            [cheshire.core :as json]
            [taoensso.timbre :as log])
  (:import [com.zaxxer.hikari HikariDataSource]
           [org.postgresql.util PGobject]
           [java.util UUID]))

;; ---------------------------------------------------------------------------
;; Configuration — everything overridable via env vars.
;; ---------------------------------------------------------------------------

(defn- env
  ([k]         (env k nil))
  ([k default] (or (System/getenv k) default)))

(defn- env-int [k default]
  (Integer/parseInt (env k (str default))))

(def ^:private db-config
  {:dbtype           "postgresql"
   :dbname           (env "FISHVISION_DB_NAME"     "fishvision")
   :host             (env "FISHVISION_DB_HOST"     "localhost")
   :port             (env-int "FISHVISION_DB_PORT" 5432)
   :user             (env "FISHVISION_DB_USER"     "postgres")  ; next.jdbc URL builder
   :username         (env "FISHVISION_DB_USER"     "postgres")  ; HikariCP
   :password         (env "FISHVISION_DB_PASSWORD" "postgres")

   ;; HikariCP pool tuning
   :maximumPoolSize   (env-int "FISHVISION_DB_POOL_SIZE" 10)
   :minimumIdle       2
   :connectionTimeout 5000
   :idleTimeout       600000
   :maxLifetime       1800000
   :poolName          "fishvision-pool"})
;; ---------------------------------------------------------------------------
;; Pooled datasource.
;; ---------------------------------------------------------------------------

(defonce ^:private datasource
  (delay (conn/->pool HikariDataSource db-config)))

(defn ds
  "Returns the pooled DataSource, creating the pool on first call."
  []
  @datasource)

(defn close-pool!
  "Graceful shutdown — call from a JVM shutdown hook."
  []
  (when (realized? datasource)
    (.close ^HikariDataSource @datasource)))

(defn ensure-datasource!
  "Retries opening the pool a few times. `delay` caches the first exception
  forever, so we work around that by branching outside the try/catch
  (recur cannot appear inside a try)."
  ([] (ensure-datasource! 5 2000))
  ([attempts delay-ms]
   (loop [n attempts]
     (let [result (try
                    (with-open [conn (jdbc/get-connection (ds))]
                      (log/info "database pool ready"
                                :dbname    (:dbname db-config)
                                :host      (:host db-config)
                                :pool-size (:maximumPoolSize db-config))
                      :ok)
                    (catch Exception e
                      {:error e}))]
       (if (= :ok result)
         true
         (let [e (:error result)]
           (if (> n 1)
             (do
               (log/warn e "database not ready, retrying"
                         :attempts-left (dec n))
               (Thread/sleep (long delay-ms))
               (recur (dec n)))
             (do
               (log/error e "database unreachable after all retries")
               (throw (ex-info "Could not connect to database"
                               {:config (dissoc db-config :password)}
                               e))))))))))

;; ---------------------------------------------------------------------------
;; Schema
;; ---------------------------------------------------------------------------

(defn init-db! []
  (jdbc/execute! (ds)
    ["CREATE TABLE IF NOT EXISTS jobs (
        id UUID PRIMARY KEY,
        image_path TEXT NOT NULL,
        status TEXT NOT NULL,
        result JSONB,
        error_message TEXT,
        created_at TIMESTAMPTZ DEFAULT now(),
        completed_at TIMESTAMPTZ
      )"])
  (println "Database initialized."))

;; ---------------------------------------------------------------------------
;; Helpers
;; ---------------------------------------------------------------------------

(def ^:private opts {:builder-fn rs/as-unqualified-kebab-maps})

(defn ->uuid [id]
  (cond
    (instance? UUID id) id
    (string? id)        (UUID/fromString id)
    :else               id))

(defn parse-result
  "Safely parse a JSONB column (PGobject) or a string into a Clojure map."
  [result]
  (when result
    (if (instance? PGobject result)
      (json/parse-string (.getValue ^PGobject result) true)
      (json/parse-string (str result) true))))

;; ---------------------------------------------------------------------------
;; Writes
;; ---------------------------------------------------------------------------

(defn insert-job! [job-id image-path]
  (jdbc/execute-one! (ds)
    (sql/format {:insert-into :jobs
                 :values [{:id (->uuid job-id)
                           :image-path image-path
                           :status "queued"}]})
    opts))

(defn set-job-processing! [job-id]
  (jdbc/execute-one! (ds)
    (sql/format {:update :jobs
                 :set    {:status "processing"}
                 :where  [:= :id (->uuid job-id)]})
    opts))

(defn save-job-result! [job-id result-map status]
  (jdbc/execute-one! (ds)
    (sql/format {:update :jobs
                 :set    {:status       status
                          :result       [:cast (if result-map
                                                 (json/generate-string result-map)
                                                 nil)
                                         :jsonb]
                          :completed-at [:now]}
                 :where  [:= :id (->uuid job-id)]})
    opts))

(defn save-job-error! [job-id error-msg]
  (jdbc/execute-one! (ds)
    (sql/format {:update :jobs
                 :set    {:status        "error"
                          :error-message error-msg
                          :completed-at  [:now]}
                 :where  [:= :id (->uuid job-id)]})
    opts))

;; ---------------------------------------------------------------------------
;; Reads
;; ---------------------------------------------------------------------------

(defn get-job [job-id]
  (when-let [row (jdbc/execute-one! (ds)
                    (sql/format {:select [:*]
                                 :from   [:jobs]
                                 :where  [:= :id (->uuid job-id)]})
                    opts)]
    (update row :result parse-result)))

(defn get-recent-jobs [limit]
  (map (fn [row] (update row :result parse-result))
       (jdbc/execute! (ds)
         (sql/format {:select   [:*]
                      :from     [:jobs]
                      :order-by [[:created-at :desc]]
                      :limit    limit})
         opts)))
