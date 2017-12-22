(ns stripe.customer
  "Stripe's Customer API."
  (:require [clojure.spec.alpha :as s]
            [stripe.http :as h]
            [stripe.schema :as ss]
            [stripe.token :as token]
            [stripe.util :as u]))

;; ==============================================================================
;; spec -------------------------------------------------------------------------
;; ==============================================================================


;; common ===============================

(s/def ::id
  string?)

(s/def ::customer-id
  ::id)

(s/def ::source-id
  ::id)

(s/def ::description
  (ss/maybe string?))

(s/def ::email
  (ss/maybe string?))

(s/def ::last4
  string?)

(s/def ::currency
  :stripe.schema/currency-id)

(s/def ::country
  string?)

(s/def ::account_number
  string?)

;; customer req =========================

(s/def :customer-req/source
  token/source?)

(s/def ::default_source
  string?)

(s/def ::customer-req
  (s/keys :opt-un [:customer-req/source ::description ::email
                   :stripe.schema/metadata ::default_source]))

;; sources ======================================================================

(s/def ::fingerprint
  (ss/maybe string?))

(s/def :source/customer
  string?)

;; bank =================================

(s/def ::status
  #{"new" "validated" "verified" "verification_failed" "errored"})

(s/def ::bank_name
  string?)

(s/def ::account_holder_type
  #{"individual" "company"})

(s/def ::account_holder_name
  string?)

;; card =================================

(s/def ::funding
  #{"credit" "debit"})

(s/def ::brand
  string?)

(s/def ::address_city
  string?)

(s/def ::address_country
  string?)

(s/def ::address_line1
  string?)

(s/def ::address_line2
  string?)

(s/def ::address_state
  string?)

(s/def ::address_zip
  string?)

(s/def ::exp_month
  integer?)

(s/def ::exp_year
  integer?)

(s/def ::name
  string?)


(defn customer-id? [x]
  (s/valid? ::customer-id x))

(defn- source [spec]
  (-> (s/keys :req-un [::id ::last4 ::country ::fingerprint
                       :source/customer])
      (s/and spec)
      (ss/metadata)))


(defmulti source-type :object)

(defmethod source-type "bank_account" [_]
  (source
   (s/keys :req-un [::bank_name ::currency ::status ::currency
                    ::account_holder_type ::account_holder_name]
           :opt-un [::routing_number])))

(defmethod source-type "card" [_]
  (source
   (s/keys :req-un [::funding ::brand])))

(s/def ::source
  (s/multi-spec source-type :object))

;; bank-source-req ======================

(s/def ::bank-source-req
  (ss/metadata
   (s/keys :opt-un [::account_holder_name ::account_holder_type])))

;; verify bank ==========================

(s/def ::deposit-amount
  (s/and integer? (u/between 1 100)))

(s/def ::amounts
  (s/cat :amount-1 ::deposit-amount :amount-2 ::deposit-amount))

;; card-source-req ======================

(s/def ::card-source-req
  (ss/metadata
   (s/keys :opt-un [::address_city ::address_country ::address_line1 ::address_line2
                    ::address_state ::address_zip ::exp_month ::exp_year ::name])))

;; customer =============================

(s/def ::created
  ss/unix-timestamp?)

(s/def ::sources
  (ss/sublist (s/* ::source)))

(s/def ::customer
  (-> (s/keys :req-un [::id :stripe.schema/livemode ::created]
              :opt-un [::default_source ::description ::email ::sources])
      (ss/stripe-object "customer")))


;; ==============================================================================
;; HTTP API  --------------------------------------------------------------------
;; ==============================================================================


;; create =============================

(defn create!
  "Creates a new customer using the Stripe API."
  [options]
  (h/post-req "customers" {:stripe-params options}))

(s/fdef create!
        :args (s/cat :opts ::customer-req)
        :ret (ss/async ::customer))


(defn add-source!
  "Add a source to a customer given the customer and source ids."
  [customer-id source-id]
  (h/post-req (format "customers/%s/sources" customer-id)
              {:stripe-params {:source source-id}}))

(s/fdef add-source!
        :args (s/cat :customer-id ::customer-id :source-id ::source-id)
        :ret (ss/async ::source))

;; fetch ================================

(defn fetch
  "Retrieves the details of an existing customer. You need only supply
  the unique customer identifier that was returned upon customer
  creation."
  [customer-id]
  (h/get-req (str "customers/" customer-id)))

(s/fdef fetch
        :args (s/cat :id ::id)
        :ret (ss/async ::customer))


(defn fetch-source
  "Get a source that's attached to a customer."
  [customer-id source-id]
  (h/get-req (format "customers/%s/sources/%s" customer-id source-id)))

(s/fdef fetch-source
        :args (s/cat :customer-id ::customer-id :source-id ::source-id)
        :ret (ss/async ::source))

;; update ===============================

(defn update!
  "Updates the specified customer by setting the values of the parameters
  passed."
  [customer-id options]
  (h/post-req (str "customers/" customer-id)
              {:stripe-params options}))

(s/fdef update!
        :args (s/cat :customer-id ::customer-id :opts ::customer-req)
        :ret (ss/async ::customer))


(defn- update-source!
  [customer-id source-id options]
  (h/post-req (format "customers/%s/sources/%s" customer-id source-id)
              {:stripe-params options}))


(defn update-bank-source!
  "Update a bank account source according to `options`."
  [customer-id source-id options]
  (update-source! customer-id source-id options))

(s/fdef update-bank-source!
        :args (s/cat :customer-id ::customer-id
                     :source-id ::source-id
                     :options ::bank-source-req)
        :ret (ss/async ::source))


(defn verify-bank-source!
  "Verify a bank account source given the deposit amounts."
  [customer-id source-id [a b :as amounts]]
  (h/post-req (format "customers/%s/sources/%s/verify"
                      customer-id source-id)
              {:stripe-params {:amounts [a b]}}))

(s/fdef verify-bank-source!
        :args (s/cat :customer-id ::customer-id
                     :source-id ::source-id
                     :amounts ::amounts)
        :ret (ss/async ::source))


(defn update-card-source!
  "Update a card source according to `options`."
  [customer-id source-id options]
  (update-source! customer-id source-id options))

(s/fdef update-card-source!
        :args (s/cat :customer-id ::customer-id
                     :source-id ::source-id
                     :options ::card-source-req)
        :ret (ss/async ::source))

;; delete ===============================

(defn delete!
  "Deletes the supplied customer."
  [customer-id]
  (h/delete-req (str "customers/" customer-id)))

(s/fdef delete!
        :args (s/cat :customer-id ::customer-id)
        :ret (ss/async ss/deleted?))


(defn delete-source!
  "Deletes the source from this customer."
  [customer-id source-id]
  (h/delete-req (format "customers/%s/sources/%s" customer-id source-id)))

(s/fdef delete-source!
        :args (s/cat :customer-id ::customer-id :source-id ::source-id)
        :ret (ss/async ss/deleted?))
