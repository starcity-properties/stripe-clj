(ns stripe.token
  (:require [clojure.spec.alpha :as s]
            [stripe.http :as h]
            [stripe.spec :as ss]
            [stripe.util :as u]))

;; ==============================================================================
;; spec =========================================================================
;; ==============================================================================


;; token returned by a call to stripe.js.
(s/def ::card-token
  string?)

;; token representing a user's bank account.
(s/def ::bank-token
  string?)

;; two digit number representing the card's expiration month.
(s/def ::exp_month
  (s/and integer? (u/between 1 13)))

;; two digit number representing the card's expiration year.
(s/def ::exp_year
  pos-int?)

(s/def ::number
  string?)

(s/def ::cvc
  string?)

(s/def ::name
  string?)

(s/def ::address_line_1
  string?)

(s/def ::address_city
  string?)

(s/def ::address_zip
  string?)

(s/def ::address_state
  string?)

(s/def ::address_country
  string?)

(s/def ::card-map
  (-> (s/keys :req-un [::number ::exp_month ::exp_year]
              :opt-un [::cvc ::name ::address_line_1 ::address_city
                       ::address_zip ::address_state ::address_country])
      (ss/stripe-object "card")))

(s/def ::card
  (s/or :token ::card-token :map ::card-map))

(s/def ::account_number
  string?)

(s/def ::routing_number
  string?)

(s/def ::currency
  string?)

(s/def ::country
  string?)

(s/def ::account_holder_name
  string?)

(s/def ::account_holder_type
  #{"individual" "company"})

(s/def ::bank-map
  (-> (s/keys :req-un [::account_number ::currency ::country]
              :opt-un [::routing_number ::account_holder_name
                       ::account_holder_type])
      (ss/stripe-object "bank_account")))

(s/def ::bank-account
  (s/or :token ::bank-token :bank-account ::bank-map))


(defn source? [x]
  (s/valid? (s/or :card ::card :bank-account ::bank-account) x))


;; ==============================================================================
;; http api =====================================================================
;; ==============================================================================


;; 0. Restructure arguments to take params and opts, not just opts within params.
;; 1. Use spec to validate that card/bank_account is either a string or map (customer is optional)
;; 2. In the body of the function, we want to check that if the token is a string, customer is required
;; 3. Update function documentation to reflect this common case


(defn create-card-token!
  "Returns a card token if successful, errors otherwise."
  ([card]
   (create-card-token! card {}))
  ([card opts]
   (h/post-req "tokens" (assoc-in opts [:params :card] card))))

(s/fdef create-card-token!
        :args (s/cat :card ::card
                     :opts (s/? h/request-options?))
        :ret (ss/async))


(defn create-bank-token!
  "Returns a bank token if successful, errors otherwise. The returned token is
  good for a one-time use - you have to attach it to a `customer` object, or its
  worthless. We hardcode Country here because the US is all that's currently
  supported by Stripe."
  ([bank]
   (create-bank-token! bank {}))
  ([bank opts]
   (let [bank (if (map? bank) (assoc bank :country "US") bank)]
     (h/post-req "tokens" (assoc-in opts [:params :bank_account] bank)))))

(s/fdef create-bank-token!
        :args (s/cat :bank ::bank-account
                     :opts (s/? h/request-options?))
        :ret (ss/async))


(defn get-token
  "Returns a card or bank object if successful, errors otherwise."
  ([token]
   (get-token token {}))
  ([token opts]
   (h/get-req (str "tokens/" token) opts)))

(s/fdef get-token
        :args (s/cat ::token (s/or :card-token ::card-token
                                   :bank-token ::bank-token)
                     ::opts (s/? h/request-options?))
        :ret (ss/async))
