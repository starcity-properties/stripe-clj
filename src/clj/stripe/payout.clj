(ns stripe.payout
  (:require [clojure.spec.alpha :as s]
            [stripe.http :as h]
            [stripe.spec :as ss]))

;; ==========================================================================
;; spec =====================================================================
;; ==========================================================================


;; common ===================================================================


;; payout ===================================================================


(s/def ::id
  string?)

(s/def ::amount
  integer?)

(s/def ::arrival_date
  ss/unix-timestamp?)

(s/def ::automatic
  boolean?)

(s/def ::balance_transaction
  string?)

(s/def ::created
  ss/unix-timestamp?)

(s/def ::currency
  ss/currency?)

(s/def ::description
  string?)

(s/def ::destination
  string?)

(s/def ::failure_balance_transaction
  (s/nilable string?))

(s/def ::failure_code
  (s/nilable string?))

(s/def ::failure_message
  (s/nilable string?))

(s/def ::livemode
  boolean?)

(s/def ::method
  #{"standard" "instant"})

(s/def ::source_type
  #{"card" "bank_account" "bitcoin_receiver" "alipay_account"})

(s/def ::statement_descriptor
  (ss/statement-descriptor?))

(s/def ::status
  #{"paid" "pending" "in_transit" "canceled" "failed"})

(s/def ::type
  #{"bank_account" "card"})

(s/def ::payout
  (-> (s/keys :req-un [::id ::amount ::arrival_date ::automatic
                       ::balance_transaction ::created ::currency
                       ::description ::destination
                       ::failure_balance_transaction ::failure_code
                       ::failure_message ::livemode ::method ::source_type
                       ::statement_descriptor ::status ::type])
      (ss/metadata)
      (ss/stripe-object "payout")))

(s/def ::payouts
  (ss/sublist ::payout))


;; create-params ============================================================


(s/def ::create-params
  (-> (s/keys :opt-un [::currency ::description ::destination ::method
                       ::source_type ::statement_descriptor])
      (ss/metadata)))


;; fetch-all ================================================================


(s/def :stripe.payout.fetch-all/arrival_date
  ss/timestamp-query?)

(s/def :stripe.payout.fetch-all/created
  ss/timestamp-query?)

(s/def ::ending_before
  string?)

(s/def ::limit
  integer?)

(s/def ::starting_after
  string?)

(s/def ::fetch-all-params
  (-> (s/keys :opt-un [:stripe.payout.fetch-all/arrival_date
                    :stripe.payout.fetch-all/created ::destination
                       ::ending_before ::limit ::starting_after ::status])
      (ss/metadata)))


;; ==========================================================================
;; http api =================================================================
;; ==========================================================================


(defn create!
  "Create a payout."
  ([amount]
   (create! amount {} {}))
  ([amount params]
   (create! amount params {}))
  ([amount {:keys [currency] :or {currency "usd"} :as params} opts]
   (let [params (assoc params :currency currency :amount amount)]
     (h/post-req "payouts" (assoc opts :params params)))))

(s/fdef create!
        :args (s/alt :unary (s/cat :amount ::amount)
                     :binary (s/cat :amount ::amount
                                    :params ::create-params)
                     :ternary (s/cat :amount ::amount
                                     :params ::create-params
                                     :opts h/request-options?))
        :ret (ss/async ::payout))


(defn fetch
  "Fetch a payout by id."
  ([payout-id]
   (fetch payout-id {}))
  ([payout-id opts]
   (h/get-req (str "payouts/" payout-id) opts)))

(s/fdef fetch
        :args (s/cat :payout-id ::id
                     :opts (s/? h/request-options?))
        :ret (ss/async ::payout))


(defn fetch-all
  "Fetch many payouts."
  ([]
   (fetch-all {} {}))
  ([params]
   (fetch-all params {}))
  ([params opts]
   (h/get-req "payouts" (assoc opts :params params))))

(s/fdef fetch-all
        :args (s/alt :nullary (s/cat)
                     :unary (s/cat :params ::fetch-all-params)
                     :binary (s/cat :params ::fetch-all-params
                                    :opts h/request-options?))
        :ret (ss/async ::payouts))


(defn update!
  "Update a payout."
  ([payout-id metadata]
   (update! payout-id metadata {}))
  ([payout-id metadata opts]
   (h/post-req (str "payouts/" payout-id)
               (assoc-in opts [:params :metadata] metadata))))

(s/fdef update!
        :args (s/cat :payout-id ::id
                     :metadata map?
                     :opts (s/? h/request-options?))
        :ret (ss/async ::payout))


(defn cancel!
  "Cancels a payout."
  ([payout-id]
   (cancel! payout-id {}))
  ([payout-id opts]
   (h/post-req (format "payouts/%s/cancel" payout-id) opts)))

(s/fdef cancel!
        :args (s/cat :payout-id ::id
                     :opts (s/? h/request-options?))
        :ret (ss/async ::payout))


(comment

  (h/use-token! "sk_test_mPUtCMOnGXJwD6RAWMPou8PH")

  (create! 500)

  (create! 500 {:method "standard"})

  (fetch "tr_18bvYhIvRccmW9nOXbU2jMkr" {:out-ch (clojure.core.async/chan)})

  (do
    (require '[clojure.spec.test.alpha :as stest])
    (stest/instrument))

  (= 10 (count (:data (fetch-all))))
  (= 3 (count (:data (fetch-all {:limit 3}))))

  ;; TODO check test once merged branch
  (fetch-all {:created {:lt 1516327443}})

  ;; TODO test cancel!
  )
