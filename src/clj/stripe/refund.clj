(ns stripe.refund
  (:require [clojure.spec.alpha :as s]
            [stripe.http :as h]
            [stripe.spec :as ss]))


;; refund =================================================================

(s/def ::id
  string?)

(s/def ::amount
  integer?)

(s/def ::balance_transaction
  string?)

(s/def ::charge
  string?)

(s/def ::created
  ss/unix-timestamp?)

(s/def ::currency
  ss/currency?)

(s/def failure_balance_transaction
  string?)

(s/def ::failure_reason
  #{"lost_or_stolen_card" "expired_or_canceled_card" "unknown"})

(s/def ::reason
  #{"duplicate" "fraudulent" "requested_by_customer"})

(s/def ::receipt_number
  string?)

(s/def ::status
  #{"succeeded" "failed" "pending" "canceled"})

(s/def ::refund
  (-> (s/keys :req-un [::id ::amount ::balance_transaction ::charge-id
                       ::created ::currency ::failure_balance_transaction
                       ::failure_reason ::reason ::receipt_number ::status])
      (ss/metadata)
      (ss/stripe-object "refund")))

(s/def ::refunds
  (ss/sublist ::refund))


;; create-params ============================================================


(s/def ::amount-pcnt
  decimal?)

(s/def ::amount
  integer?)

(s/def ::reason
  string?)

(s/def ::refund_application_fee
  boolean?)

(s/def ::reverse_transfer
  boolean?)

(s/def ::refund-params
  (-> (s/keys :opt-un [::amount ::reason ::refund_application_fee
                       ::reverse_transfer])
      (ss/metadata)))


;; fetch-all ================================================================


(s/def ::fetch-all-params
  (-> (s/keys :opt-un [::charge ::ending_before ::limit ::starting_after])
      (ss/metadata)))


;; ============================================================================
;; http api ===================================================================
;; ============================================================================


(defn create!
  "Issue a refund on a charge."
  ([charge-id]
   (create! charge-id {} {}))
  ([charge-id params]
   (create! charge-id params {}))
  ([charge-id params opts]
   (let [params' (assoc params :charge charge-id)]
     (h/post-req "refunds" (assoc opts :params params')))))

(s/fdef create!
        :args (s/alt :unary (s/cat :charge-id ::charge)
                     :binary (s/cat :charge-id ::charge
                                    :params ::refund-params)
                     :ternary (s/cat :charge-id ::charge
                                     :params ::refund-params
                                     :opts (s/? h/request-options?))
                     :ret (ss/async ::refund)))


(defn fetch
  "Fetch a refund."
  ([refund-id]
   (fetch refund-id {}))
  ([refund-id opts]
   (h/get-req (str "refunds/" refund-id) opts)))

(s/fdef fetch
        :args (s/cat :refund-id ::id
                     :opts (s/? h/request-options?))
        :ret (ss/async ::refund))


(defn fetch-all
  "Fetch all refunds previously created."
  ([]
   (fetch-all {} {}))
  ([params]
   (fetch-all params {}))
  ([params opts]
   (h/get-req "refunds" (assoc opts :params params))))

(s/fdef fetch-all
        :args (s/alt :nullary (s/cat)
                     :unary (s/cat :params ::fetch-all-params)
                     :binary (s/cat :params ::fetch-all-params
                                    :opts h/request-options?))
        :ret (ss/async ::refunds))


(defn update!
  "Update a refund."
  ([refund-id metadata]
   (update! refund-id metadata {}))
  ([refund-id metadata opts]
   (h/post-req (str "refunds/" refund-id) (assoc-in opts [:params :metadata] metadata))))

(s/fdef update!
        :args (s/cat :refund-id ::id
                     :metadata ss/metadata?
                     :opts (s/? h/request-options?))
        :ret (ss/async ::refund))

(comment

  (h/use-token! "sk_test_mPUtCMOnGXJwD6RAWMPou8PH")

  (h/use-token! nil)

  (create! "re_1Bo4SwIvRccmW9nObjukcHHf")

  (fetch "re_1Bo4SwIvRccmW9nObjukcHHf" {:out-ch (clojure.core.async/chan)})

  (fetch-all {:created {:lt 1516327443}})

  ;; TODO test update!

  )
