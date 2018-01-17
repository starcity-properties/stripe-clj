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


(s/def ::statement-descriptor
  (s/and string? #(<= (count %) 22)))

(s/def ::charge-amount
  (s/and integer? #(>= % 50)))          ; minimum is 50 cents


(def statement-descriptor?
  "Is the argument a valid statement descriptor."
  (partial s/valid? ::statement-descriptor))


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
  (ss/maybe ::charge-amount))

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

(s/def ::charges
  (ss/sublist ::charge))

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

(s/def ::transfer_group
  string?)

(s/def ::on_behalf_of
  string?)

(s/def ::statement_descriptor
  ::statement-descriptor)

(S/def ::charge-req
  (-> (s/keys :req-un [::amount]
              :opt-un [::currency ::source ::customer ::description ::capture
                       ::application_fee ::receipt_email ::destination
                       ::transfer_group ::on_behalf_of ::statement_descriptor])
      (ss/metadata)))


(def charge-req?
  "Is the argument a valid charge request?"
  (partial s/valid? ::charge-req))


(S/def ::fetch-req
  (-> (s/keys :req-un [::charge-id]
              :opt-un [::limit])
      (ss/metadata)))


(def fetch-req?
  "Is the argument a valid fetch request?"
  (partial s/valid? ::fetch-req))


;; ==============================================================================
;; http api =====================================================================
;; ==============================================================================


;; TODO: create a charge requires either customer or source. ok as is?

(defn create!
  "Create a charge."
  [{{currency :currency} :params, :as options}]
  (h/post-req "charges"
              (assoc-in options [:params :currency] (or currency "usd"))))

(s/fdef create!
        :args (s/cat :opts (h/request-options? ::charge-req))
        :ret (ss/async ::charge))


(defn fetch
  "Returns a channel containing the charge if it exists, or an error if it
  doesn't."
  ([charge-id]
   (fetch charge-id {}))
  ([charge-id opts]
   (h/get-req (str "charges/" charge-id) opts)))

(s/fdef fetch
        :args (s/cat :charge-id string?
                     :opts (s/? (h/request-options?)))
        :ret (ss/async string?))

;; TODO: update!
(defn update!
  "Updates a charge with values for any of the following arguments: customer, description, fraud-details, metadata, receipt-email, shipping. If any parameters are invalid, returns an error."
  ([charge-id]
   (update charge-id {}))
  ([charge-id opts]
   (h/put-req (str "charges/" charge-id) opts))
  )

(s/fdef update
        :args (s/cat))


;; TODO: capture!
(defn capture! []
  )

;; TODO: fetch-all
(defn fetch-all
  "Returns a channel containing all charges if they exist up to a certain limit, if specified. If charges do not exist, return will be an empty vector."
  ([]
   (fetch-all {}))
  ([opts])
  (h/get-req "charges" opts))


(s/fdef fetch-all
        :args (s/cat :opts (s/? integer?))
        :ret (ss/async ::charges))



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

  (h/use-token! nil)

  ;; (h/use-connect-account! "acct_191838JDow24Tc1a")
  (h/use-connect-account! nil)

  ;; asynchronous
  (let [c (clojure.core.async/chan)]
    (create! {:params {:customer    "cus_BzZW6T3NzySJ5E"
                       :amount      500
                       :description "Test platform charge"}
              :out-ch c})
    c)


  (defn random-function []
    (create! {:params {:customer    "cus_BzZW6T3NzySJ5E"
                       :amount      500
                       :description "Test platform charge"}}))

  ;; synchronous
  (h/with-token "sk_test_mPUtCMOnGXJwD6RAWMPou8PH"
    (random-function)
    )

  (random-function)


  (h/with-connect-account "acct_191838JDow24Tc1a"
    (create! {:params {:customer    "cus_BU7S7e46Y0wed9"
                       :amount      500
                       :description "Test connect charge"}}))

  (h/with-connect-account "acct_191838JDow24Tc1a"
    (amount-refunded (fetch "py_1BbyfuJDow24Tc1arEHZ7Ecl")))

  )
