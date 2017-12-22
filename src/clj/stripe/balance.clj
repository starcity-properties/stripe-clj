(ns stripe.balance
  "Functions for Stripe's Balance API."
  (:require [clojure.spec.alpha :as s]
            [stripe.http :as h]
            [stripe.schema :as ss]))

;; =============================================================================
;; Spec
;; =============================================================================

(s/def ::amount
  pos-int?)

(s/def ::currency
  :stripe.schema/currency-id)

(s/def ::source_types
  (s/map-of keyword? integer?))

(s/def ::balance-amount
  (s/keys :req-un [::amount ::currency] :opt-un [::source_types]))

(s/def ::balance-tx-id
  string?)

(s/def ::available
  (s/+ ::balance-amount))

(s/def ::pending
  (s/+ ::balance-amount))

(s/def ::balance
  (-> (s/keys :req-un [::livemode
                       ::available
                       ::pending
                       ::object])
      (ss/stripe-object "balance")))

(s/def :fee-details/type
  string?)

(s/def ::description
  (ss/maybe string?))

(s/def ::application
  (ss/maybe string?))

(s/def ::fee-details
  (s/keys :req-un [::amount ::currency :fee-details/type ::description ::application]))

(s/def ::transaction-type
  #{"charge" "refund" "adjustment" "application_fee"
    "application_fee_refund" "transfer" "transfer_cancel"
    "transfer_failure" "payment" "payment_refund"})

(s/def ::id
  ::balance-tx-id)

(s/def ::available_on
  ss/unix-timestamp?)

(s/def ::created
  ss/unix-timestamp?)

(s/def ::fee
  integer?)

(s/def ::fee_details
  (s/+ ::fee-details))

(s/def ::net
  integer?)

(s/def ::status
  #{"pending" "available"})

(s/def :balance/type
  ::transaction-type)

(s/def ::source
  (s/or :string string? :map map?))

(s/def ::balance-tx
  (-> (s/keys :req-un [::id ::amount ::available_on ::created ::currency ::fee
                       ::fee_details ::net ::status :balance/type ::description
                       ::source ::object])
      (s/and (ss/stripe-object "balance_transaction"))))

(s/def ::starting-after
  string?)

(s/def ::limit
  pos-int?)

(s/def ::history-options
  (s/and (s/keys :opt-un [::starting-after ::limit])
         :stripe.http/request-options))


;; =============================================================================
;; HTTP API
;; =============================================================================


(defn get-balance
  "Returns a channel containing the current account balance, based
   on the API key that was used to make the request."
  ([]
   (get-balance {}))
  ([opts]
   (h/get-req "balance" opts)))

(s/fdef get-balance
        :args (s/cat :opts (s/? :stripe.http/request-options))
        :ret (ss/async ::balance))


(defn get-history
  ([]
   (get-history {}))
  ([opts]
   (h/get-req "balance/history"
              (-> {:stripe-params (merge {:limit (:limit opts 100)}
                                         (when-let [sa (:starting-after opts)]
                                           {:starting_after sa}))}
                  (merge opts)))))

(s/fdef get-history
        :args (s/cat :opts (s/? ::history-options))
        :ret (ss/async (ss/sublist (s/* ::balance-tx))))


(defn get-all-history
  "Synchronously fetches all transaction history for an optional Connect `account`; otherwise
  will fetch from the platform account."
  [& [account-id]]
  (letfn [(fetch [& [after]]
            (get-history (merge
                          {}
                          (when-some [a account-id] {:account account-id})
                          (when-some [a after] {:starting-after a}))))]
    (loop [page    (fetch)
           history []]
      (let [data (:data page)
            all  (concat history (:data page))]
        (if (:has_more page)
          (recur (fetch (:id (last data))) all)
          all)))))

(s/fdef get-all-history
        :args (s/cat :account (s/? string?))
        :ret (s/* ::balance-tx))


(defn get-balance-tx
  "Returns a channel that contains the balance transaction referenced
  by the supplied ID, or an error if it doesn't exist."
  ([id]
   (get-balance-tx id {}))
  ([id opts]
   (h/get-req (str "balance/history/" id) opts)))

(s/fdef get-balance-tx
        :args (s/cat :transaction-id ::balance-tx-id
                     :opts (s/? :stripe.http/request-options))
        :ret (ss/async ::balance-tx))


;; =============================================================================
;; Selectors
;; =============================================================================


(defn available-amount
  "Returns the current amount in the given currency or USD currently
  available in our account."
  [{:keys [available]} & [currency]]
  (let [currency (or currency "usd")]
    (:amount (first (filter (comp #{currency} :currency) available)))))

(s/fdef available-amount
        :args (s/cat :balance ::balance :currency (s/? ::currency))
        :ret pos-int?)


(defn pending-amount
  "Returns the current amount in the given currency or USD pending in our account."
  [{:keys [pending]} & [currency]]
  (let [currency (or currency "usd")]
    (:amount (first (filter (comp #{currency} :currency) pending)))))

(s/fdef pending-amount
        :args (s/cat :balance ::balance :currency (s/? ::currency))
        :ret pos-int?)


(defn tx-fee [tx]
  (:fee tx))

(s/fdef tx-fee
        :args (s/cat :transaction ::balance-tx)
        :ret integer?)
