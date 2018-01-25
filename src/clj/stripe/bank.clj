(ns stripe.bank
  (:require [clojure.spec.alpha :as s]
            [stripe.http :as h]
            [stripe.spec :as ss]))


;; ==========================================================================
;; spec =====================================================================
;; ==========================================================================


;; bank account =============================================

(s/def ::id
  string?)

(s/def ::customer-id
  ::id)

(s/def ::account
  string?)

(s/def ::account_holder_name
  string?)

(s/def ::account_holder_type
  #{"individual" "company"})

(s/def ::bank_name
  string?)

(s/def ::country
  (ss/country?)

(s/def ::currency
  ss/currency?)

(s/def ::default_for_currency
  boolean?)

(s/def ::fingerprint
  string?)

(s/def ::routing_number
  string?)

(s/def ::status
  #{"new" "validated" "verified" "verification_failed" "errored"})

(s/def ::bank-account
  (-> (s/keys :req-un [::id ::account ::account_holder_name
                       ::account_holder_type ::bank_name ::country
                       ::currency ::default_for_currency ::fingerprint
                       :stripe.spec/last4 ::routing_number ::status])
      (ss/metadata)
      (ss/stripe-object "bank_account")))

(s/def ::bank-accounts
  (ss/sublist ::bank-account))

(s/def ::source-map
  (-> (s/keys :req-un [::account_number ::country ::currency]
              :opt-un [::account_holder_name ::account_holder_type
                       ::routing_number])
      (ss/metadata)))

(s/def ::source
  (s/or :source-id string? :source-map ::source-map))

;; update-params ==============================================================

(s/def ::update-params
  (-> (s/keys :opt-un [::account_holder_name ::account_holder_type]))
      (ss/metadata))

;; verify-params ==============================================================

(s/def ::amounts
  (ss/sublist pos-int?))

(s/def ::verification_method
  string?)

(s/def ::verify-params
  (-> (s/keys :opt-un [::amounts ::verification_method]))
      (ss/metadata))

;; fetch-all-params ===========================================================

(s/def ::ending_before
  string?)

(s/def ::limit
  integer?)

(s/def ::starting_after
  string?)

(s/def ::fetch-all-params
  (s/keys :req-un [::customer-id]
          :opt-un [::ending_before ::limit ::starting_after]))


;; ============================================================================
;; http api ===================================================================
;; ============================================================================


(defn create!
  "Create a bank account, specifying a customer."
  ([customer-id source]
   (create! customer-id source {}))
  ([customer-id source opts]
   (let [params {:source (assoc source :object "bank_account")}]
     (h/post-req (format "customers/%s/sources" customer-id)
                 (update opts :params merge params)))))

(s/fdef create!
        :args (s/cat :customer-id ::customer-id
                     :source ::source
                     :opts (s/? h/request-options?))
        :ret (ss/async ::bank-account))


(defn fetch
  "Fetch a bank account."
  ([customer-id bank-account-id]
   (fetch customer-id bank-account-id {}))
  ([customer-id bank-account-id opts]
   (h/post-req (format "customers/%s/sources/%s" customer-id bank-account-id) opts)))

(s/fdef fetch
        :args (s/cat :customer-id ::customer-id
                     :bank-account-id ::id
                     :opts h/request-options?)
        :ret (ss/async ::bank-account))


(defn fetch-all
  "Fetch many bank accounts, specifying a single customer."
  ([customer-id]
   (fetch-all customer-id {} {}))
  ([customer-id params]
   (fetch-all customer-id params {}))
  ([customer-id params opts]
   (h/get-req (format "customers/%s/sources" customer-id)
              (assoc opts :params params))))

(s/fdef fetch-all
        :args (s/alt :unary (s/cat :customer-id ::customer-id)
                     :binary (s/cat :customer-id ::customer-id
                                    :params ::fetch-all-params)
                     :ternary (s/cat :customer-id ::customer-id
                                     :params ::fetch-all-params
                                     :opts h/request-options?))
        :ret (ss/async ::bank-accounts))


(defn update!
  "Update a bank account."
  ([customer-id bank-account-id]
   (update! customer-id bank-account-id {} {}))
  ([customer-id bank-account-id params]
   (update! bank-account-id params {}))
  ([customer-id bank-account-id params opts]
   (let [params (assoc params :customer-id customer-id
                              :bank-account-id bank-account-id)]
     (h/post-req (format "customers/%s/sources/%s" customer-id bank-account-id)
                 (assoc opts :params params)))))

(s/fdef update!
        :args (s/alt :binary (s/cat :customer-id ::customer-id
                                    :bank-account-id ::id)
                     :ternary (s/cat :customer-id ::customer-id
                                     :bank-account-id ::id
                                     :params ::update-params)
                     :quaternary (s/cat :customer-id ::customer-id
                                        :bank-account-id ::id
                                        :params ::update-params
                                        :opts h/request-options?))
        :ret (ss/async ::bank-account))


(defn verify!
  "Verify a bank account."
  ([customer-id bank-account-id]
   (verify! customer-id bank-account-id {} {}))
  ([customer-id bank-account-id params]
   (verify! customer-id bank-account-id params {}))
  ([customer-id bank-account-id params opts]
   (let [params (assoc params :customer-id customer-id
                              :bank-account-id bank-account-id)]
     (h/post-req (format "customers/%s/sources/%s/verify" customer-id bank-account-id)
                 (assoc opts :params params)))))

(s/fdef verify!
        :args (s/alt :binary (s/cat :customer-id ::customer-id
                                    :bank-account-id ::id)
                     :ternary (s/cat :customer-id ::customer-id
                                     :bank-account-id ::id
                                     :params ::verify-params)
                     :quaternary (s/cat :customer-id ::customer-id
                                     :bank-account-id ::id
                                     :params ::verify-params
                                     :opts h/request-options?))
        :ret (ss/async ::bank-account))


(defn delete!
  "Delete a bank account from a customer."
  ([customer-id bank-account-id]
   (delete! bank-account-id {}))
  ([customer-id bank-account-id opts]
   (h/post-req (format "customers/%s/sources/%s" customer-id bank-account-id))))

(s/fdef delete!
        :args (s/cat :customer-id ::customer-id
                     :bank-account-id ::id
                     :opts (s/? h/request-options?))
        :ret (ss/async ss/deleted?))


(comment

  (do
    (h/use-token! "sk_test_mPUtCMOnGXJwD6RAWMPou8PH")
    (def test-customer "cus_CBaoFshG350779")
    (def test-bank-account {:object "bank_account"
                            :account_number "test_account"
                            :country "US"
                            :currency "usd"
                            :account_holder_name "Jane Austen"
                            :account_holder_type "individual"
                            :routing_number "110000000"}))

  ;; TODO resolve ==> :message "You cannot use a live bank account number when making transfers or debits in test mode"

  (create! test-customer test-bank-account)

  (fetch test-customer test-bank-account)

  (fetch-all test-customer)

  (delete! test-customer test-bank-account)

  ;; TODO (verify! x x) ; should return bank account with status of `verified`

)
