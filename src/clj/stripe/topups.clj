(ns stripe.topups
  (:require [clojure.spec.alpha :as s]
            [stripe.http :as h]
            [stripe.spec :as ss]
            [toolbelt.spec :as ts]
            [toolbelt.core :as tb]))


;; ==========================================================================
;; spec =====================================================================
;; ==========================================================================

;; top up object ============================================================

(s/def ::id
  string?)

(s/def ::object
  string?)

(s/def ::amount
  integer?)

(s/def ::balance_transaction
  string?)

(s/def ::created
  ts/unix-timestamp?)

(s/def ::currency
  ss/currency?)

(s/def ::description
  string?)

(s/def ::expected_availability_date
  integer?)

(s/def ::failure_code
  string?)

(s/def ::failure_message
  string?)

(s/def ::livemode
  ss/livemode?)

(s/def ::metadata
  map?)

(s/def ::source
  (s/or :string string?
        :map map?))

(s/def ::statement_descriptor
  (s/and string?
         #(<= (count %) 15)))

(s/def ::status
  (s/and string?
         #{"canceled" "failed" "pending" "reversed" "succeeded"}))

(s/def ::transfer_group
  string?)

(s/def ::top-up
  (s/keys :opt-un [::id ::object ::amount ::balance_transaction
                   ::created ::currency ::description ::expected_availability_date
                   ::failure_code ::failure_message ::livemode ::metadata ::source
                   ::statement_descriptor ::status ::transfer_group]))


;; create params ============================================================

(s/def ::source_id
  string?)

(s/def ::create-params
  (s/keys :opt-un [::description ::metadata ::source_id ::statement_descriptor ::transfer_group]))


;; fetch params =============================================================

(s/def :stripe.topups.amount/gt
  integer?)

(s/def :stripe.topups.amount/gte
  integer?)

(s/def :stripe.topups.amount/lt
  integer?)

(s/def :stripe.topups.amount/lte
  integer?)

(s/def ::amount-query
  (s/or :amount integer?
        :query-map (s/keys :opt-un [:stripe.topups.amount/gt
                                    :stripe.topups.amount/gte
                                    :stripe.topups.amount/lt
                                    :stripe.topups.amount/lte])))

(defn amount-query? [x]
  (s/valid? ::amount-query x))


(s/def :stripe.topups.fetch-all/amount
  amount-query?)

(s/def :stripe.topups.fetch-all/created
  ss/timestamp-query?)

(s/def ::ending_before
  string?)

(s/def ::starting_after
  string?)

(s/def ::limit
  (s/and integer?
         #(<= 1 % 100)))

(s/def ::fetch-all-params
  (s/keys :opt-un [:stripe.topups.fetch-all/amount :stripe.topups.fetch-all/created
                   ::ending_before ::limit ::starting_after ::status]))


;; ==========================================================================
;; http api =================================================================
;; ==========================================================================

(defn create!
  "Top up the balance of an account."
  ([amount currency]
   (create! amount currency {} {}))
  ([amount currency params]
   (create! amount currency params {}))
  ([amount currency params opts]
   (let [params (tb/assoc-when
                  params
                  :currency currency
                  :amount amount
                  :source (:source-id params))]
     (h/post-req "topups" (assoc opts :params params)))))

(s/fdef create!
        :args (s/alt :binary (s/cat :amount ::amount
                                    :currency ::currency)
                     :ternary (s/cat :amount ::amount
                                     :currency ::currency
                                     :params ::create-params)
                     :quaternary (s/cat :amount ::amount
                                        :currency ::currency
                                        :params ::create-params
                                        :opts h/request-options?))
        :ret (ts/async ::top-up))


(defn fetch
  "Retrieves the details of a top-up that has previously been created."
  ([topup-id]
   (fetch topup-id {}))
  ([topup-id opts]
   (h/get-req (str "topups/" topup-id) opts)))

(s/fdef fetch
        :args (s/cat :transfer-id ::id
                     :opts h/request-options?)
        :ret (ts/async ::top-up))


(defn fetch-all
  "Returns a list of top-ups."
  ([]
   (fetch-all {} {}))
  ([params]
   (fetch-all params {}))
  ([params opts]
   (h/get-req "topups" (assoc opts :params params))))

(s/fdef fetch-all
        :args (s/alt :nullary (s/cat)
                     :unary (s/cat :params ::fetch-all-params)
                     :binary (s/cat :params ::fetch-all-params
                                    :opts h/request-options?))
        :ret (ts/async (ss/sublist ::top-up)))


(defn update!
  "Updates the metadata of a top-up. Other top-up details are not editable by design."
  ([transfer-id metadata]
   (update! transfer-id metadata {}))
  ([transfer-id metadata opts]
   (h/post-req (str "topups/" transfer-id)
               (assoc-in opts [:params :metadata] metadata))))

(s/fdef update!
        :args (s/cat :transfer-id ::id
                     :metadata ss/metadata?
                     :opts (s/? h/request-options?))
        :ret (ts/async ::top-up))


(defn cancel!
  "Cancels a top-up. Only pending top-ups can be canceled."
  ([transfer-id]
   (cancel! transfer-id {}))
  ([transfer-id opts]
   (h/post-req (str "topups/" transfer-id "/cancel") opts)))

(s/fdef cancel!
        :args (s/cat :transfer-id ::id
                     :opts (s/? h/request-options?))
        :ret (ts/async ::top-up))
