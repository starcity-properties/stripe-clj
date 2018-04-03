(ns stripe.invoiceitem
  (:require [clojure.spec.alpha :as s]
            [stripe.customer :as customer]
            [stripe.http :as h]
            [stripe.spec :as ss]
            [toolbelt.spec :as ts]))


;; ==========================================================================
;; spec =====================================================================
;; ==========================================================================


;; invoiceitem ==============================================================

(s/def ::id
  string?)

(s/def ::unit-amount
  integer?)

(s/def ::quantity
  integer?)

(s/def ::amount
  integer?)

(s/def ::currency
  ss/currency?)

(s/def ::customer
  string?)

(s/def ::date
  ss/timestamp-query?)

(s/def ::description
  string?)

(s/def ::discountable
  boolean?)

(s/def ::invoice
  string?)

(s/def ::livemode
  ss/livemode?)

(s/def ::start
  ss/timestamp-query?)

(s/def ::end
  ss/timestamp-query?)

(s/def ::period
  (s/keys :req-un [::start ::end]))

(s/def ::plan
  map?)

(s/def ::proration
  boolean?)

(s/def ::subscription
  string?)

(s/def ::subscription_item
  string?)

(s/def ::invoiceitem
  (-> (s/keys :req-un [::id ::amount ::currency ::customer ::date ::description
                       ::discountable ::invoice ::livemode ::period ::plan
                       ::proration ::quantity ::subscription ::subscription_item
                       ::unit_amount])
      (ss/metadata)
      (ss/stripe-object "invoiceitem")))

(s/def ::invoiceitems
  (ss/sublist ::invoiceitem))


;; create-params ============================================================


(s/def ::create-params
  (-> (s/keys :req-un [::currency ::customer]
              :opt-un [::amount ::description ::discountable ::invoice
                       ::quantity ::subscription ::unit_amount])
      (ss/metadata)))


;; update-params ============================================================


(s/def ::update-params
  (-> (s/keys :opt-un [::amount ::description ::discountable
                       ::quantity ::unit_amount])
      (ss/metadata)))


;; fetch-all-params =========================================================


(s/def ::created
  ss/timestamp-query?)

(s/def ::ending_before
  string?)

(s/def ::limit
  integer?)

(s/def ::starting_after
  string?)

(s/def ::fetch-all-params
  (-> (s/keys :opt-un [::created ::customer ::ending_before ::invoice
                       ::limit ::starting_after])
      (ss/metadata)))


;; ==========================================================================
;; http api =================================================================
;; ==========================================================================


(defn create!
  "Create an `invoiceitem`."
  ([customer params]
   (create! customer params {}))
  ([customer {:keys [currency] :or {currency "usd"} :as params} opts]
   (let [params (assoc params :currency currency :customer customer)]
     (h/post-req "invoiceitems" (assoc opts :params params)))))

(s/fdef create!
        :args (s/alt :unary (s/cat :customer ::customer)
                     :binary (s/cat :customer ::customer
                                    :params ::create-params)
                     :ternary (s/cat :customer ::customer
                                     :params ::create-params
                                     :opts (s/? h/request-options?)))
        :ret (ts/async ::invoiceitem))


(defn fetch
  "Fetch an `invoiceitem` by id."
  ([invoiceitem-id]
   (fetch invoiceitem-id {}))
  ([invoiceitem-id opts]
   (h/get-req (str "invoiceitem/" invoiceitem-id) opts)))

(s/fdef fetch
        :args (s/cat :invoiceitem-id ::id
                     :opts (s/? h/request-options?))
        :ret (ts/async ::invoiceitem))


(defn fetch-all
  "Fetch many `invoiceitem`s."
  ([]
   (fetch-all {} {}))
  ([params]
   (fetch-all params {}))
  ([params opts]
   (h/get-req "invoiceitems" (assoc opts :params params))))

(s/fdef fetch-all
        :args (s/alt :nullary (s/cat)
                     :unary (s/cat :params ::fetch-all-params)
                     :binary (s/cat :params ::fetch-all-params
                                    :opts h/request-options?))
        :ret (ts/async ::invoiceitems))


(defn update!
  "Update an `invoiceitem`."
  ([invoiceitem-id params]
   (update! invoiceitem-id params {}))
  ([invoiceitem-id params opts]
   (h/post-req (str "invoiceitems/" invoiceitem-id) (assoc-in opts :params params))))

(s/fdef update!
        :args (s/cat :invoiceitem-id ::id
                     :params ::update-params
                     :opts (s/? h/request-options?))
        :ret (ts/async ::invoiceitem))


(defn delete!
  "Delete the supplied `invoiceitem`."
  ([invoiceitem-id]
   (delete! invoiceitem-id {}))
  ([invoiceitem-id opts]
   (h/delete-req (str "invoiceitems/" invoiceitem-id) opts)))

(s/fdef delete!
        :args (s/cat :invoiceitem-id ::id
                     :opts (s/? h/request-options?))
        :ret (ts/async ss/deleted?))


;; ==================================================================================


(comment
  (h/use-token! "sk_test_mPUtCMOnGXJwD6RAWMPou8PH")

  (h/use-token! nil)

  (let [c (customer/create! {:customer    "cus_BzZW6T3NzySJ5E"
                             :description "Test create customer"})]
    (create! (:id c) {:amount 400}))

)
