(ns stripe.transfer
  (:require [clojure.spec.alpha :as s]
            [stripe.http :as h]
            [stripe.spec :as ss]
            [toolbelt.spec :as ts]))


;; ==========================================================================
;; spec =====================================================================
;; ==========================================================================


;; transfer =================================================================

(s/def ::id
  string?)

(s/def ::amount
  integer?)

(s/def ::amount_reversed
  integer?)

(s/def ::balance_transaction
  string?)

(s/def ::created
  ts/unix-timestamp?)

(s/def ::currency
  ss/currency?)

(s/def ::description
  (s/nilable string?))

(s/def ::destination
  string?)

(s/def ::destination_payment
  string?)

(s/def ::livemode
  ss/livemode?)

(s/def ::reversed
  boolean?)

(s/def ::source_transaction
  (s/nilable string?))

(s/def ::source_type
  #{"card" "bank_account" "bitcoin_receiver" "alipay_account"})

(s/def ::transfer_group
  (s/nilable string?))

(s/def ::transfer
  (-> (s/keys :req-un [::id ::amount ::amount_reversed ::balance_transaction ::created
                       ::currency ::description ::destination ::destination_payment ::livemode
                       ::reversed ::source_transaction ::source_type ::transfer_group])
      (ss/metadata)
      (ss/stripe-object "transfer")))

(s/def ::transfers
  (ss/sublist ::transfer))


;; create-params ============================================================


(s/def ::create-params
  (-> (s/keys :opt-un [::amount ::currency ::destination
                       ::source_transaction ::transfer_group])
      (ss/metadata)))


;; fetch-all-params =========================================================


(s/def :stripe.transfer.fetch-all/created
  ss/timestamp-query?)

(s/def ::ending_before
  string?)

(s/def ::limit
  integer?)

(s/def ::starting_after
  string?)

(s/def ::fetch-all-params
  (-> (s/keys :opt-un [:stripe.transfer.fetch-all/created ::destination
                       ::ending_before ::limit ::starting_after ::transfer_group])
      (ss/metadata)))


;; ==========================================================================
;; http api =================================================================
;; ==========================================================================


(defn create!
  "Create transfer object to send funds from Stripe account to a connected account."
  ([amount destination]
   (create! amount destination {} {}))
  ([amount destination params]
   (create! amount destination params {}))
  ([amount destination {:keys [currency] :or {currency "usd"} :as params} opts]
   (let [params (assoc params :currency currency :amount amount :destination destination)]
      (h/post-req "transfers" (assoc opts :params params)))))

(s/fdef create!
        :args (s/alt :binary (s/cat :amount ::amount
                                    :destination ::destination)
                     :ternary (s/cat :amount ::amount
                                     :destination ::destination
                                     :params ::create-params)
                     :quaternary (s/cat :amount ::amount
                                        :destination ::destination
                                        :params ::create-params
                                        :opts h/request-options?))
        :ret (ts/async ::transfer))


(defn fetch
  "Fetch transfer object."
  ([transfer-id]
   (fetch transfer-id {}))
  ([transfer-id opts]
   (h/get-req (str "transfers/" transfer-id) opts)))

(s/fdef fetch
        :args (s/cat :transfer-id ::id
                     :opts h/request-options?)
        :ret (ts/async ::transfer))


(defn fetch-all
  "Fetch a list of existing transfers sent to connected accounts."
  ([]
   (fetch-all {} {}))
  ([params]
   (fetch-all params {}))
  ([params opts]
   (h/get-req "transfers" (assoc opts :params params))))

(s/fdef fetch-all
        :args (s/alt :nullary (s/cat)
                     :unary (s/cat :params ::fetch-all-params)
                     :binary (s/cat :params ::fetch-all-params
                                    :opts h/request-options?))
        :ret (ts/async ::transfers))


(defn update!
  "Update a transfer object."
  ([transfer-id metadata]
   (update! transfer-id metadata {}))
  ([transfer-id metadata opts]
   (h/post-req (str "transfers" transfer-id)
               (assoc-in opts [:params :metadata] metadata))))

(s/fdef update!
        :args (s/cat :transfer-id ::id
                     :metadata ss/metadata?
                     :opts (s/? h/request-options?))
        :ret (ts/async ::transfer))


(comment)
