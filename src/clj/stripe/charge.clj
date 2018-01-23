(ns stripe.charge
  (:require [clojure.spec.alpha :as s]
            [stripe.customer :as customer]
            [stripe.http :as h]
            [stripe.spec :as ss]
            [stripe.token :as token]))

;; ==============================================================================
;; spec =========================================================================
;; ==============================================================================


;; common =======================================================================


(s/def ::charge-amount
  (s/and integer? #(>= % 50)))          ; minimum is 50 cents


(def charge-amount?
  "Is the argument a valid charge amount?"
  (partial s/valid? ::charge-amount))


;; charge =======================================================================


(s/def ::id
  string?)

(s/def ::amount
  ::charge-amount)

(s/def ::amount_refunded
  (s/or :pos-int pos-int? :zero zero?))

(s/def ::application
  (ss/maybe string?))

(s/def ::application_fee
  (ss/maybe ::charge-amount?))

(s/def ::balance_transaction
  (ss/maybe string?))

(s/def ::captured
  boolean?)

(s/def ::created
  ss/unix-timestamp?)

(s/def ::customer
  (ss/maybe string?))

(s/def ::description
  (ss/maybe string?))

(s/def ::charge
  (-> (s/keys :req-un [::id ::amount ::amount_refunded ::application ::application_fee
                       ::balance_transaction ::captured ::created ::currency ::customer
                       ::description
                       ;; ::destination ::dispute ::failure_code ::failure_message
                       ;; ::fraud_details ::invoice ::livemode ::on_behalf_of ::order ::outcome
                       ;; ::paid
                       ])
      (ss/metadata)
      (ss/stripe-object "charge")))


(def charge?
  "Is the argument a charge?"
  (partial s/valid? ::charge))


;; request ======================================================================


(s/def ::charge-id
  string?)

(s/def ::id
  ::charge-id)

(s/def ::amount
  ::charge-amount)

(s/def ::currency
  ss/currency?)

(s/def ::source
  token/source?)

(s/def ::customer
  customer/customer-id?)

(s/def ::description
  string?)

(s/def ::capture
  boolean?)

(s/def ::receipt_email
  string?)

(s/def ::account
  string?)

(s/def ::destination
  (s/keys :req-un [::account] :opt-un [::amount]))

(s/def ::application_fee
  ::charge-amount)

(s/def ::transfer_group
  string?)

(s/def ::on_behalf_of
  string?)

(s/def ::statement_descriptor
  ss/statement-descriptor?)

(s/def ::charge-req
  (-> (s/keys :req-un [::amount]
              :opt-un [::currency ::source ::customer ::description ::capture
                       ::application_fee ::receipt_email ::destination
                       ::transfer_group ::on_behalf_of ::statement_descriptor])
      (ss/metadata)))


(def charge-req?
  "Is the argument a valid charge request?"
  (partial s/valid? ::charge-req))


;; ==============================================================================
;; http api =====================================================================
;; ==============================================================================


(defn create!
  [{{currency :currency} :params, :as options}]
  (h/post-req "charges"
              (assoc-in options [:params :currency] (or currency "usd"))))

(s/fdef create!
        :args (s/cat :opts (h/request-options? ::charge-req))
        :ret (ss/async ::charge))


(defn fetch
  "Returns a channel containing the charge if it exists, or an error
  if it doesn't."
  ([charge-id]
   (fetch charge-id {}))
  ([charge-id opts]
   (h/get-req (str "charges/" charge-id) opts)))

(s/fdef fetch
        :args (s/cat :charge-id string?
                     :opts (s/? h/request-options?))
        :ret (ss/async ::charge))


;; ==============================================================================
;; selectors ====================================================================
;; ==============================================================================


(defn amount-available
  "Returns the amount that the charge is actually worth, or the amount
  available for further refunds."
  [charge]
  (- (:amount charge) (:amount_refunded charge 0)))

(s/fdef amount-available
        :args (s/cat :charge ::charge)
        :ret (s/or :zero zero? :pos-int pos-int?))


(defn amount-refunded
  "Returns the total amount that was refunded for the charge."
  [charge]
  (:amount_refunded charge 0))

(s/fdef amount-refunded
        :args (s/cat :charge ::charge)
        :ret (s/or :zero zero? :pos-int pos-int?))


;; ==============================================================================


(comment
  (h/use-token! "sk_test_mPUtCMOnGXJwD6RAWMPou8PH")

  ;; (h/use-connect-account! "acct_191838JDow24Tc1a")
  (h/use-connect-account! nil)

  (create! {:params {:customer    "cus_BzZW6T3NzySJ5E"
                     :amount      500
                     :description "Test platform charge"}})

  (h/with-connect-account "acct_191838JDow24Tc1a"
    (create! {:params {:customer    "cus_BU7S7e46Y0wed9"
                       :amount      500
                       :description "Test connect charge"}}))

  (h/with-connect-account "acct_191838JDow24Tc1a"
    (amount-refunded (fetch "py_1BbyfuJDow24Tc1arEHZ7Ecl")))

  )
