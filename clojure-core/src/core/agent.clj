(ns core.agent
  (:require [malli.core :as m]
            [malli.error :as me]
            [taoensso.timbre :as log]))

(def AgentOutputSchema
  [:map
   [:state [:enum :fresh :highly-fresh :not-fresh]]
   [:action [:enum :pass :review]]
   [:confidence :double]])

;; Margin thresholds, tuned to MFED embeddings.
;; Margin = top1_similarity - top2_similarity.
;; Measured on MFED: correct predictions avg margin ~0.04,
;; wrong predictions avg margin ~0.01. 0.03 separates them reasonably.
(def ^:private pass-margin-threshold 0.03)

(defn evaluate-decision
  "Thin agent wrapper around the k-NN primary decision.

   Decision rule:
     pass    when k-NN margin (top1 - top2 similarity) exceeds threshold
             AND k-NN agrees with CNN
     review  everything else

   Reasoning: on clustered embeddings (MFED), raw cosine similarity is
   uninformative — every query is close to every class.  Margin (how much
   the top-1 beats the runner-up) is the honest uncertainty signal."
  [{:keys [cnn knn analysis]}]
  (let [knn-pred   (or (:prediction knn) :not-fresh)
        knn-sim    (double (or (:confidence knn) 0.0))
        knn-margin (double (or (:margin knn) 0.0))
        cnn-pred   (:prediction cnn)
        agree?     (= knn-pred cnn-pred)
        confident? (>= knn-margin pass-margin-threshold)
        action     (if (and confident? agree?) :pass :review)
        decision   {:state      knn-pred
                    :action     action
                    ;; report margin as the confidence value — it's the
                    ;; meaningful number
                    :confidence knn-margin
                    :similarity knn-sim
                    :agree?     agree?
                    :evidence   {:knn        knn
                                 :cnn-pred   cnn-pred
                                 :cnn-logits (:logits cnn)
                                 :features   analysis}}]
    (if (m/validate AgentOutputSchema
                    (select-keys decision [:state :action :confidence]))
      (do (log/debug "agent decision"
                     :knn-pred   (name knn-pred)
                     :knn-margin knn-margin
                     :knn-sim    knn-sim
                     :cnn-pred   (some-> cnn-pred name)
                     :agree?     agree?
                     :action     (name action))
          {:status :ok :result decision})
      {:status :error
       :message (me/humanize
                 (m/explain AgentOutputSchema
                            (select-keys decision [:state :action :confidence])))})))
