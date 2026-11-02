(ns core.dashboard
  (:require [hiccup.page :refer [html5]]
            [hiccup.core :refer [html]]
            [clojure.string :as str]))

(defn- display-image [job]
  (or (:original-filename job)
      (some-> (:image-path job) (str/split #"/") last)
      "—"))

(defn- display-prediction [job]
  (or (some-> (get-in job [:result :agent :result :state]) name)
      (some-> (get-in job [:result :knn :prediction]) name)
      "—"))

(defn- display-similarity [job]
  (when-let [s (get-in job [:result :knn :confidence])]
    (format "%.3f" (double s))))

(defn- display-margin [job]
  (when-let [m (get-in job [:result :knn :margin])]
    (format "%.3f" (double m))))

(defn- display-action [job]
  (or (some-> (get-in job [:result :agent :result :action]) name)
      "—"))

(defn- display-duration [job]
  (if (and (:created-at job) (:completed-at job))
    (str (- (.getTime ^java.sql.Timestamp (:completed-at job))
            (.getTime ^java.sql.Timestamp (:created-at job)))
         " ms")
    "—"))

(defn render-jobs-table [jobs]
  (html
   [:div {:id "jobs-container"
          :hx-get "/dashboard/jobs"
          :hx-trigger "every 1s"
          :hx-swap "outerHTML"}
    [:h2 "Recent Jobs (" (count jobs) " total)"]
    [:table
     (into [:tr
            [:th "Job ID"]
            [:th "Status"]
            [:th "Image"]
            [:th "Prediction"]
            [:th "Similarity"]
            [:th "Margin"]
            [:th "Action"]
            [:th "Time"]]
           (for [job jobs]
             (let [agree? (get-in job [:result :agent :result :agree?])]
               [:tr
                [:td [:code (subs (str (:id job)) 0 8)]]
                [:td {:class (name (keyword (:status job)))
                      :style "font-weight:bold;"}
                 (str/upper-case (:status job))]
                [:td (display-image job)]
                [:td (display-prediction job)]
                [:td (or (display-similarity job) "—")]
                [:td (or (display-margin job) "—")]
                [:td {:style (when-not agree? "color:#b45309;font-weight:bold;")}
                 (str/upper-case (display-action job))]
                [:td (display-duration job)]])))]]))

(defn render-page [jobs]
  (html5
   [:head [:title "FishVision Dashboard"]
          [:script {:src "https://unpkg.com/htmx.org@1.9.12"}]
          [:style "body{font-family:sans-serif;margin:2em}
                   table{border-collapse:collapse;margin:1em 0;width:100%}
                   th,td{padding:8px 12px;border:1px solid #ccc;text-align:left}
                   .queued{color:gray}.processing{color:blue;animation:blink 1s infinite}
                   .done{color:green}.error{color:red}
                   @keyframes blink { 50% { opacity: 0.5; } }"]]
   [:body
    [:h1 "FishVision Monitoring"]
    [:h2 "System"]
    [:ul [:li (str "CPU Cores: " (.availableProcessors (Runtime/getRuntime)))]]
    [:h2 "Model Metadata"]
    [:table
     [:tr [:th "Name"] [:th "Version"] [:th "Input Shape"] [:th "Classes"]]
     [:tr [:td "MobileNetV3-Small (CNN)"]
          [:td "FFE-trained"]
          [:td "[1, 5, 224, 224]"]
          [:td "fresh, highly-fresh, not-fresh"]]
     [:tr [:td "k-NN on CNN embeddings"]
          [:td "600 MFED refs"]
          [:td "[1, 5, 224, 224]"]
          [:td "fresh, highly-fresh, not-fresh"]]]
    (render-jobs-table jobs)]))
