(ns stripe.customer
  "Stripe's Customer API."
  (:require [clojure.spec.alpha :as s]
            [stripe.http :as h]
            [stripe.spec :as ss]
            [stripe.token :as token]
            [stripe.util :as u]))

;; ==============================================================================
;; spec =========================================================================
;; ==============================================================================


;; common =======================================================================


(s/def ::id
  string?)

(s/def ::customer-id
  ::id)

(s/def ::source-id
  ::id)

(s/def ::description
  (s/nilable string?))

(s/def ::email
  (s/nilable string?))

(s/def ::currency
  ss/currency?)

(s/def ::country
  ss/country?)

(s/def ::account_number
  string?)


;; customer request =============================================================


(s/def ::limit
  integer?)

(s/def ::starting_after
  string?)

(s/def ::ending_before
  string?)

(s/def :customer-req/source
  any?)

(s/def ::default_source
  (s/nilable string?))

(s/def ::customer-req
  (-> (s/keys :opt-un [:customer-req/source ::description ::email ::default_source])
      ss/metadata))

(s/def ::all-customers-req
  (-> (s/keys :opt-un [::limit ::created ::email ::starting_after ::ending_before])
      (ss/metadata)))

(def create-customer-params?
  (partial s/valid? ::customer-req))

;; sources ======================================================================


(s/def ::fingerprint
  (s/nilable string?))

(s/def :source/customer
  string?)


;; bank =========================================================================


(s/def ::status
  #{"new" "validated" "verified" "verification_failed" "errored"})

(s/def ::bank_name
  string?)

(s/def ::account_holder_type
  #{"individual" "company"})

(s/def ::account_holder_name
  string?)


;; card =========================================================================


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
  (-> (s/keys :req-un [::id :stripe.spec/last4 ::country ::fingerprint
                       :source/customer])
      (s/and spec)
      (ss/metadata)))


(defmulti source-type :object)

(defmethod source-type "bank_account" [_]
  (source
   (s/keys :req-un [::bank_name ::currency ::status
                    ::account_holder_type ::account_holder_name]
           :opt-un [::routing_number])))

(defmethod source-type "card" [_]
  (source
   (s/keys :req-un [::funding ::brand])))

(s/def ::source
  (s/multi-spec source-type :object))

(s/def ::source-type
  (s/or :source ::source :source-id ::source-id))

;; bank source request ==========================================================


(s/def ::bank-source-req
  (ss/metadata
   (s/keys :opt-un [::account_holder_name ::account_holder_type])))


;; verify bank ==================================================================


(s/def ::deposit-amount
  (s/and integer? (u/between 1 100)))

(s/def ::amounts
  (s/cat :amount-1 ::deposit-amount :amount-2 ::deposit-amount))


;; card source request ==========================================================


(s/def ::card-source-req
  (ss/metadata
   (s/keys :opt-un [::address_city ::address_country ::address_line1 ::address_line2
                    ::address_state ::address_zip ::exp_month ::exp_year ::name])))


;; customer =====================================================================


(s/def ::created
  ss/unix-timestamp?)

(s/def ::sources
  (ss/sublist (s/* ::source)))

(s/def ::customer
  (-> (s/keys :req-un [::id :stripe.spec/livemode ::created]
              :opt-un [::default_source ::description ::email ::sources])
      (ss/stripe-object "customer")))

(s/def ::customers
  (ss/sublist ::customer))


;; ==============================================================================
;; HTTP API  ====================================================================
;; ==============================================================================


;; create! ======================================================================


(defn create!
  "Creates a new customer using the Stripe API."
  ([params]
   (create! params {}))
  ([params opts]
   (h/post-req "customers" (assoc opts :params params))))

(s/fdef create!
        :args (s/cat :params ::customer-req
                     :opts (s/? h/request-options?))
        :ret (ss/async ::customer))


(defn add-source!
  "Add a source to a customer given the customer and source ids."
  ([customer-id source]
   (add-source! customer-id source {}))
  ([customer-id source opts]
   (h/post-req (format "customers/%s/sources" customer-id)
               (assoc opts :params {:source source}))))

(s/fdef add-source!
        :args (s/cat :customer-id ::customer-id
                     :source ::source-type
                     :opts (s/? h/request-options?))
        :ret (ss/async ::source))


;; fetch ========================================================================


(defn fetch
  "Retrieves the details of an existing customer. You need only supply
  the unique customer identifier that was returned upon customer
  creation."
  ([customer-id]
   (fetch customer-id {}))
  ([customer-id opts]
   (h/get-req (str "customers/" customer-id) opts)))

(s/fdef fetch
        :args (s/cat :id ::id
                     :opts (s/? h/request-options?))
        :ret (ss/async ::customer))


(defn fetch-source
  "Get a source that's attached to a customer."
  ([customer-id source-id]
   (fetch-source customer-id source-id {}))
  ([customer-id source-id opts]
   (h/get-req (format "customers/%s/sources/%s" customer-id source-id) opts)))

(s/fdef fetch-source
        :args (s/cat :customer-id ::customer-id
                     :source-id ::source-id
                     :opts (s/? h/request-options?))
        :ret (ss/async ::source))


;; update! ======================================================================


(defn update!
  "Updates the specified customer by setting the values of the parameters
  passed."
  ([customer-id params]
   (update! customer-id params {}))
  ([customer-id params opts]
   (h/post-req (str "customers/" customer-id) (assoc opts :params params))))

(s/fdef update!
        :args (s/cat :customer-id ::customer-id
                     :params ::customer-req
                     :opts (s/? h/request-options?))
        :ret (ss/async ::customer))


(defn- update-source!
  [customer-id source-id opts]
  (h/post-req (format "customers/%s/sources/%s" customer-id source-id)
              opts))


(defn update-bank-source!
  "Update a bank account source according to `opts`."
  ([customer-id source-id params]
   (update-bank-source! customer-id source-id params {}))
  ([customer-id source-id params opts]
   (update-source! customer-id source-id (assoc opts :params params))))

(s/fdef update-bank-source!
        :args (s/cat :customer-id ::customer-id
                     :source-id ::source-id
                     :params ::bank-source-req
                     :opts (s/? h/request-options?))
        :ret (ss/async ::source))


(defn verify-bank-source!
  "Verify a bank account source given the deposit amounts."
  ([customer-id source-id amounts]
   (verify-bank-source! customer-id source-id amounts {}))
  ([customer-id source-id [a b] opts]
   (h/post-req (format "customers/%s/sources/%s/verify"
                       customer-id source-id)
               (assoc opts :params {:amounts [a b]}))))

(s/fdef verify-bank-source!
        :args (s/cat :customer-id ::customer-id
                     :source-id ::source-id
                     :amounts ::amounts
                     :opts (s/? h/request-options?))
        :ret (ss/async ::source))


(defn update-card-source!
  "Update a card source according to `opts`."
  ([customer-id source-id params]
   (update-card-source! customer-id source-id params {}))
  ([customer-id source-id params opts]
   (update-source! customer-id source-id (assoc opts :params params))))

(s/fdef update-card-source!
        :args (s/cat :customer-id ::customer-id
                     :source-id ::source-id
                     :params ::card-source-req
                     :opts (s/? h/request-options?))
        :ret (ss/async ::source))


;; delete! ======================================================================


(defn delete!
  "Deletes the supplied customer."
  ([customer-id]
   (delete! customer-id {}))
  ([customer-id opts]
   (h/delete-req (str "customers/" customer-id) opts)))

(s/fdef delete!
        :args (s/cat :customer-id ::customer-id
                     :opts (s/? h/request-options?))
        :ret (ss/async ss/deleted?))


(defn delete-source!
  "Deletes the source from this customer."
  ([customer-id source-id]
   (delete-source! customer-id source-id))
  ([customer-id source-id opts]
   (h/delete-req (format "customers/%s/sources/%s" customer-id source-id) opts)))

(s/fdef delete-source!
        :args (s/cat :customer-id ::customer-id
                     :source-id ::source-id
                     :opts (s/? h/request-options?))
        :ret (ss/async ss/deleted?))


;; fetch-all ========================================================================


(defn fetch-all
  "Returns a list of customers sorted by creation date, most recent first."
  ([params]
   (fetch-all params {}))
  ([params opts]
   (h/get-req "customers" (assoc opts :params params))))

(s/fdef fetch-all
        :args (s/cat :params ::all-customers-req
                     :opts (s/? h/request-options?))
        :ret (ss/async ::customers))


;; TODO: make `search-by-email` function


;; ==================================================================================


(comment
  (h/use-token! "sk_test_mPUtCMOnGXJwD6RAWMPou8PH")

  (h/use-token! nil)

  (do
    (require '[clojure.spec.test.alpha :as stest])
    (stest/instrument))

  ;; asynchronous
  (let [c (clojure.core.async/chan)]
    (create! {:out-ch c} {:customer    "cus_BzZW6T3NzySJ5E"
                          :description "Test create customer"})
    c)


  (defn random-function []
    (create! {:customer    "cus_BzZW6T3NzySJ5E"
              :description "Test create customer"}))


  ;; The three ways to use an api token in requests

  (create! {:customer    "cus_BzZW6T3NzySJ5E"
            :description "Test create customer"})

  ;; synchronous
  (h/with-token "sk_test_mPUtCMOnGXJwD6RAWMPou8PH"
    (create! {:customer    "cus_BzZW6T3NzySJ5E"
              :description "Test create customer"}))

  (create! {:customer    "cus_BzZW6T3NzySJ5E"
            :description "Test create customer"}
           {:token (config/stripe-private-key config)})

  (add-source! "cus_BzZW6T3NzySJ5E" "test")

  )
