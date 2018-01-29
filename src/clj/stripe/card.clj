(ns stripe.card
  (:require [clojure.spec.alpha :as s]
            [stripe.spec :as ss]
            [stripe.util :as u]
            [stripe.http :as h]))


;; ==============================================================================
;; spec =========================================================================
;; ==============================================================================

(s/def ::check
  #{"pass" "fail" "unavailable" "unchecked"})

(s/def ::id
  string?)

(s/def ::account
  string?)

(s/def ::number
  string?)

(s/def ::address_city
  (s/nilable string?))

(s/def ::address_country
  (s/nilable string?))

(s/def ::address_line1
  (s/nilable string?))

(s/def ::address_line1_check
  (s/nilable ::check))

(s/def ::address_line2
  (s/nilable string?))

(s/def ::state
  (s/nilable string?))

(s/def ::address_zip
  (s/nilable string?))

(s/def ::address_zip_check
  (s/nilable string?))

(s/def ::available_payout_methods
  ::check)

(s/def ::payout-method
  #{"standard" "instant"})

(s/def ::available_payout_methods
  (s/+ ::payout-method))

(s/def ::brand
  #{"Visa" "American Express" "MasterCard" "Discover" "JCB" "Diners Club" "Unknown"})

(s/def ::country
  (s/and string? #(= 2 (count %))))

(s/def ::currency
  ss/currency?)

(s/def ::customer
  (s/nilable string?))

(s/def ::cvc_check
  (s/nilable ::check))

(s/def ::default_for_currency
  boolean?)

(s/def ::dynamic_last4
  (s/nilable string?))

(s/def ::exp_month
  (s/and integer? (u/between 1 13)))

(s/def ::exp_year
  integer?)

(s/def ::fingerprint
  string?)

(s/def ::funding
  #{"credit" "debit" "prepaid" "unknown"})

(s/def ::name
  (s/nilable string?))

(s/def ::recipient
  string?)

(s/def ::tokenization_method
  (s/nilable #{"apple_pay" "android_pay"}))

(s/def ::card
  (-> (s/keys :req-un [::id ::brand ::country ::customer ::exp_month
                       ::exp_year ::fingerprint ::funding :stripe.spec/last4 ]
              :opt-un [::account ::address_city ::address_country
                       ::address_line1 ::address_line1_check
                       ::address_line2 ::address_state ::address_zip
                       ::address_zip ::address_zip_check ::payout-method
                       ::customer ::cvc_check ::dynamic_last4 ::name
                       ::recipient ::tokenization_method])
      (ss/metadata)
      (ss/stripe-object "card")))

(s/def ::source-map
  (-> (s/keys :req-un [::exp_month ::exp_year ::number ::cvc]
              :opt-un [::address_city ::address_country ::address_line1
                       ::address_line2 ::address_state ::address_zip
                       ::currency ::default_for_currency ::name])
      (ss/metadata)))

(s/def ::source
  (s/or :source-id string? :source-map ::source-map))

;; update-params ========================

(s/def ::update-params
  (-> (s/keys :opt-un [::address_city ::address_country ::address_line1
                       ::address_line2 ::address_state ::address_zip
                       ::exp_month ::exp_year])
      (ss/metadata)))

;; fetch-all-params =====================

(s/def ::limit
  ss/limit?)

(s/def ::ending_before
  string?)

(s/def ::starting_after
  string?)

(s/def ::fetch-all-params
  (s/keys :opt-un [::ending_before ::limit ::starting_after]))

(s/def ::cards
  (ss/sublist ::card))

(def cards?
  (partial s/valid? ::cards))

;; ==============================================================================
;; http api =====================================================================
;; ==============================================================================


(defn create!
  "Create a new card source."
  ([customer-id source]
   (create! customer-id source {}))
  ([customer-id source opts]
   (if (map? source)
     (let [params {:source (assoc source :object "card")}])
     (let [params {:source source}]))
   (h/post-req (format "customers/%s/sources" customer-id)
               (update opts :params merge params))))

(s/fdef create!
        :args (s/cat :customer-id ::id
                     :source ::source
                     :opts (s/? h/request-options?))
        :ret (ss/async ::card))


(defn fetch
  "Retrieve details of a customer's card."
  ([customer-id source-id]
   (fetch customer-id source-id {}))
  ([customer-id source-id opts]
   (h/get-req (format "customers/%s/sources/%s" customer-id source-id) opts)))

(s/fdef fetch
        :args (s/cat :customer-id ::id
                     :source-id ::id
                     :opts (s/? h/request-options?))
        :ret (ss/async ::card))


(defn update!
  "Updates details of a customer's card by setting the values of the parameters passed and returns a card object."
  ([customer-id card-id params]
   (update! customer-id card-id params {}))
  ([customer-id card-id params opts]
   (h/post-req (format "customers/%s/sources/%s" customer-id card-id)
               (assoc opts :params params))))

(s/fdef update!
        :args (s/cat :customer-id ::id
                     :card-id ::id
                     :params ::update-params
                     :opts (s/? h/request-options?))
        :ret (ss/async ::card))


(defn delete!
  "Deletes a card from a customer or recipient"
  ([customer-id card-id]
   (delete! customer-id card-id  {}))
  ([customer-id card-id opts]
   (h/delete-req (format "customers/%s/sources/%s" customer-id card-id) opts)))

(s/fdef delete!
        :args (s/cat :customer-id ::id
                     :card-id ::id
                     :opts (s/? h/request-options?))
        :ret (ss/async ss/deleted?))


(defn fetch-all
  "Retrieves a list of cards belonging to a customer or recipient."
  ([customer-id]
   (fetch-all customer-id {} {}))
  ([customer-id params]
   (fetch-all customer-id params {}))
  ([customer-id params opts]
   (h/get-req (format "customers/%s/sources?object=card" customer-id)
              (assoc opts :params params))))

(s/fdef fetch-all
        :args (s/cat :customer-id ::id
                     :params (s/? ::fetch-all-params)
                     :opts (s/? h/request-options?))
        :ret (ss/async ::cards))

(comment

  ;; Stripe test secret
  (do
    (h/use-token! "sk_test_mPUtCMOnGXJwD6RAWMPou8PH")
    (def test-customer "cus_CBaoFshG350779")
    (def test-card {:object "card" :exp_month 8 :exp_year 2021 :number "4242424242424242" :cvc 242}))

  (create! test-customer {:object "card" :exp_month 8 :exp_year 2021 :number "4242424242424242" :cvc 242})

  (update! test-customer "card_1BnE8LIvRccmW9nO9QaA2nfv"
           {:exp_year 2022})

  (fetch test-customer "card_1BnE8LIvRccmW9nO9QaA2nfv")

  (fetch-all test-customer)
  (fetch-all test-customer {})

  (delete! test-customer "card_1BnXjMIvRccmW9nOAAJqgBcT")

  (do
    (require '[clojure.spec.test.alpha :as stest])
    (stest/instrument))


  ;; (defn create-card-ret )

  (h/use-connect-account! "acct_191838JDow24Tc1a")
  ;; (h/use-connect-account! nil)

  )
