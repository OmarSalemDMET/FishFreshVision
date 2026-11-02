(ns core.agent-test
  (:require [clojure.test :refer [deftest is testing]]
            [malli.core :as m]
            [malli.error :as me]
            [core.agent :as agent]))

;; ---------------------------------------------------------------------------
;; Schema validation
;; ---------------------------------------------------------------------------

(deftest agent-output-schema-test
  (testing "accepts valid agent outputs"
    (is (m/validate agent/AgentOutputSchema
                    {:state :fresh :action :pass :confidence 0.5}))
    (is (m/validate agent/AgentOutputSchema
                    {:state :highly-fresh :action :trigger-fft :confidence 0.12}))
    (is (m/validate agent/AgentOutputSchema
                    {:state :not-fresh :action :pass :confidence 0.999})))

  (testing "rejects unknown state"
    (is (not (m/validate agent/AgentOutputSchema
                         {:state :unknown :action :pass :confidence 0.5}))))

  (testing "rejects unknown action"
    (is (not (m/validate agent/AgentOutputSchema
                         {:state :fresh :action :skip :confidence 0.5}))))

  (testing "rejects non-double confidence"
    (is (not (m/validate agent/AgentOutputSchema
                         {:state :fresh :action :pass :confidence "high"}))))

  (testing "humanized explain is a non-empty structure for invalid data"
    (let [explain (me/humanize
                   (m/explain agent/AgentOutputSchema
                              {:state :bogus :action :pass :confidence 0.5}))]
      (is (map? explain))
      (is (contains? explain :state)))))

;; ---------------------------------------------------------------------------
;; Agent decision logic
;; ---------------------------------------------------------------------------

(deftest evaluate-cnn-output-high-confidence-test
  (testing "high softmax confidence produces :pass"
    ;; logits [10 0 0] -> softmax confidence ~0.9999
    (let [result (agent/evaluate-cnn-output
                  {:prediction :fresh
                   :logits [10.0 0.0 0.0]})]
      (is (= :ok (:status result)))
      (is (= :fresh (get-in result [:result :state])))
      (is (= :pass (get-in result [:result :action])))
      (is (<= 0.99 (get-in result [:result :confidence]))))))

(deftest evaluate-cnn-output-low-confidence-test
  (testing "uniform logits produce low confidence and :trigger-fft"
    (let [result (agent/evaluate-cnn-output
                  {:prediction :not-fresh
                   :logits [0.0 0.0 0.0]})]
      (is (= :ok (:status result)))
      (is (= :not-fresh (get-in result [:result :state])))
      (is (= :trigger-fft (get-in result [:result :action])))
      (is (< (get-in result [:result :confidence]) 0.99)))))

(deftest evaluate-cnn-output-confidence-math-test
  (testing "confidence equals softmax of the max logit"
    ;; logits [1 0 0] -> exp(1)/(exp(1)+1+1) = e/(e+2) ≈ 0.5761
    (let [result (agent/evaluate-cnn-output
                  {:prediction :fresh
                   :logits [1.0 0.0 0.0]})
          expected (/ Math/E (+ Math/E 2.0))]
      (is (m/validate agent/AgentOutputSchema (:result result)))
      (is (< (Math/abs (- expected (get-in result [:result :confidence])))
             1e-6)))))

(deftest evaluate-cnn-output-all-states-test
  (testing "every class label round-trips through the schema"
    (doseq [[prediction logits]
            [[:fresh [10.0 0.0 0.0]]
             [:highly-fresh [0.0 10.0 0.0]]
             [:not-fresh [0.0 0.0 10.0]]]]
      (let [result (agent/evaluate-cnn-output
                    {:prediction prediction :logits logits})]
        (is (= :ok (:status result)))
        (is (= prediction (get-in result [:result :state])))
        (is (m/validate agent/AgentOutputSchema (:result result)))))))

(deftest evaluate-cnn-output-error-shape-test
  (testing "when sci or schema fails the function returns :status :error"
    ;; Negative confidence cannot pass schema, so we force a schema failure
    ;; by asking for an impossible state via a broken prediction.  The agent
    ;; always builds from the prediction keyword, so use an invalid keyword
    ;; that will not be in the enum.
    (let [result (agent/evaluate-cnn-output
                  {:prediction :not-a-class
                   :logits [1.0 0.0 0.0]})]
      (is (= :error (:status result)))
      ;; :message is either a string (from ex-message) or a map
      ;; (from me/humanize).  Both are valid, so accept either.
      (is (some? (:message result)))
      (is (or (string? (:message result))
              (map? (:message result)))))))
