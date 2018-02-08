(ns stripe.plan
  (:require [clojure.spec.alpha :as s]
            [stripe.spec :as ss]
            [stripe.http :as h]
            [clojure.core.async :as a]))


;; ==============================================================================
;; spec =========================================================================
;; ==============================================================================


(s/def ::id
  string?)

(s/def ::amount
  (s/or :positive pos-int? :zero zero?))

(s/def ::created
  ss/unix-timestamp?)

(s/def ::currency
  ss/currency?)

(s/def ::interval
  #{"day" "week" "month" "year"})

(s/def ::interval_count
  pos-int?)

(s/def ::livemode
  boolean?)

(s/def ::name
  string?)

(s/def ::statement_descriptor
  (s/nilable ss/statement-descriptor?))

(s/def ::trial_period_days
  (s/nilable (s/or :positive pos-int? :zero zero?)))

(s/def ::plan
  (-> (s/keys :req-un [::currency ::interval ::name ::id ::amount
                       ::created ::interval_count ::livemode]
              :opt-un [::statement_descriptor ::trial_period_days])
      (ss/metadata)
      (ss/stripe-object "plan")))

(def plan?
  (partial s/valid? ::plan))

;; create-params ========================

(s/def ::create-params
  (s/keys :opt-un [::id ::currency ::interval_count ::statement_descriptor]))

;; update-params ========================

(s/def ::update-params
  (-> (s/keys :opt-un [::name ::statement_descriptor])
      (ss/metadata)))

;; fetch-all-params =====================

(s/def ::limit
  ss/limit?)

(s/def ::ending_before
  string?)

(s/def ::starting_after
  string?)

(s/def ::fetch-all-params
  (-> (s/keys :opt-un [::ending_before ::limit ::starting_after])))

(s/def ::plans
  (ss/sublist ::plan))

(def plans?
  (partial s/valid? ::plans))

;; ==============================================================================
;; http api =====================================================================
;; ==============================================================================


(defn create!
  "Create a plan."
  ([name interval amount]
   (create! name interval amount {} {}))
  ([name interval amount params]
   (create! name interval amount params {}))
  ([name interval amount params opts]
   (let [params' (merge params {:name     name
                                :interval interval
                                :amount   amount
                                :currency (or (:currency params) "usd")})]
     (h/post-req "plans" (assoc opts :params params')))))

(s/fdef create!
        :args (s/alt :ternary (s/cat :name ::name
                                     :interval ::interval
                                     :amount ::amount)
                     :quaternary (s/cat :name ::name
                                        :interval ::interval
                                        :amount ::amount
                                        :params ::create-params)
                     :quinary (s/cat :name ::name
                                     :interval ::interval
                                     :amount ::amount
                                     :params ::create-params
                                     :opts h/request-options?))
        :ret (ss/async ::plan))


(defn fetch
  "Fetch a plan."
  ([plan-id]
   (fetch plan-id {}))
  ([plan-id opts]
   (h/get-req (str "plans/" plan-id) opts)))

(s/fdef fetch
        :args (s/cat :plan-id ::id
                     :opts (s/? h/request-options?))
        :ret (ss/async ::plan))


(defn update!
  "Update a plan."
  ([plan-id params]
   (update! plan-id params {}))
  ([plan-id params opts]
   (h/post-req (str "plans/" plan-id)
               (assoc opts :params params))))

(s/fdef update!
        :args (s/cat :plan-id ::id
                     :params ::update-params
                     :opts (s/? h/request-options?))
        :ret (ss/async ::plan))


(defn delete!
  "Delete a plan."
  ([plan-id]
   (delete! plan-id {}))
  ([plan-id opts]
   (h/delete-req (str "plans/" plan-id) opts)))

(s/fdef delete!
        :args (s/cat :plan-id ::id
                     :opts (s/? h/request-options?))
        :ret (ss/async ss/deleted?))


(defn fetch-all
  "Fetch all plans."
  ([]
   (fetch-all {} {}))
  ([params]
   (fetch-all params {}))
  ([params opts]
   (h/get-req "plans" (assoc opts :params params))))

(s/fdef fetch-all
        :args (s/alt :nullary (s/cat)
                     :unary (s/cat :params ::fetch-all-params)
                     :binary (s/cat :params ::fetch-all-params
                                     :opts h/request-options?))
        :ret (ss/async ::plans))


(comment
  (h/use-token! "sk_test_mPUtCMOnGXJwD6RAWMPou8PH")

  (create! "some-plan-name" "year" 1199)
  (create! "some-plan-name" "year" 0)

  (fetch "plan_CBd1wYdinPsthF")

  (update! "plan_CBd1wYdinPsthF" {:name "new name" :metadata {:a 1}})

  (delete! "plan_CBd1wYdinPsthF")

  (fetch-all)
  (fetch-all {:limit 1})

  (do
    (require '[clojure.spec.test.alpha :as stest])
    (stest/instrument))

  )
