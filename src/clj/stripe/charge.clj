(ns stripe.charge
  (:require [clojure.spec.alpha :as s]
            [stripe.customer :as customer]
            [stripe.http :as h]
            [stripe.spec :as ss]
            [stripe.token :as token]
            [toolbelt.spec :as ts]))

;; ==============================================================================
;; spec =========================================================================
;; ==============================================================================


;; common =======================================================================


(s/def ::statement_descriptor
  ss/statement-descriptor?)

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
  (s/nilable string?))

(s/def ::application_fee
  (s/or :pos-int pos-int? :zero zero?))

(s/def ::balance_transaction
  (s/nilable string?))

(s/def ::captured
  boolean?)

(s/def ::created
  ts/unix-timestamp?)

(s/def ::ending_before
  string?)

(s/def ::starting_after
  string?)

(s/def ::limit
  integer?)

(s/def ::customer
  (s/nilable string?))

(s/def ::description
  (s/nilable string?))

(s/def ::status
  #{"succeeded" "pending" "failed"})

(s/def ::charge
  (-> (s/keys :req-un [::id ::amount ::amount_refunded ::application ::application_fee
                       ::balance_transaction ::captured ::created ::currency ::customer
                       ::description ::status
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
  (s/or :token token/source? :string string? :map map?))

(s/def ::customer
  customer/customer-id?)

(s/def ::description
  (s/nilable string?))

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
  ss/statement-descriptor?)

(s/def ::fraud_details
  map?)

(s/def ::shipping
  (s/nilable map?))

(s/def ::charge-params
  (letfn [(has-source-or-customer? [x]
            (or (contains? x :source) (contains? x :customer)))]
    (-> (s/keys :opt-un [::currency ::source ::customer ::description ::capture
                         ::application_fee ::receipt_email ::destination
                         ::transfer_group ::on_behalf_of ::statement_descriptor])
        (s/and has-source-or-customer?)
        (ss/metadata))))


(s/def ::update-params
  (-> (s/keys :req-un [::charge-id]
              :opt-un [::customer ::description ::fraud_details ::receipt_email ::shipping])
      (ss/metadata)))


(s/def ::capture-params
  (-> (s/keys :req-un [::charge-id]
              :opt-un [::amount ::application_fee ::destination ::receipt_email ::statement_descriptor])
      (ss/metadata)))


(s/def ::fetch-all-params
  (-> (s/keys :opt-un [::limit ::created ::customer ::transfer_group ::starting_after ::ending_before])
      (ss/metadata)))


(def charge-params?
  "Is the argument a valid charge request?"
  (partial s/valid? ::charge-params))


(def update-params?
  "Is the argument a valid update request?"
  (partial s/valid? ::update-params))


(def capture-params?
  "Is the argument a valid capture request?"
  (partial s/valid? ::capture-params))


(def fetch-all-params?
  "Is the argument a valid fetch all request?"
  (partial s/valid? ::fetch-all-params))


;; ==============================================================================
;; http api =====================================================================
;; ==============================================================================


(defn create!
  "Create a charge."
  ([amount params]
   (create! amount params {}))
  ([amount {:keys [currency] :or {currency "usd"} :as params} opts]
   (let [params (assoc params :currency currency :amount amount)]
     (h/post-req "charges" (assoc opts :params params)))))

(s/fdef create!
        :args (s/cat :amount ::amount
                     :params ::charge-params
                     :opts (s/? h/request-options?))
        :ret (ts/async ::charge))


(defn fetch
  "Returns a channel containing the charge if it exists, or an error if it
  doesn't."
  ([charge-id]
   (fetch charge-id {}))
  ([charge-id opts]
   (h/get-req (str "charges/" charge-id) opts)))

(s/fdef fetch
        :args (s/cat :charge-id string?
                     :opts (s/? h/request-options?))
        :ret (ts/async string?))


(defn update!
  "Returns an updated charge with values for any of the following arguments: customer, description, fraud details, metadata, receipt email, shipping. If any parameters are invalid, returns an error."
  ([charge-id params]
   (update! charge-id {}))
  ([charge-id params opts]
   (h/post-req (str "charges/" charge-id) (assoc opts :params params))))

(s/fdef update!
        :args (s/cat :charge-id string?
                     :params ::update-params
                     :opts (s/? h/request-options?))
        :ret (ts/async string?))


(defn capture!
  "Returns an updated charge with captured property set to true. If charge is already refunded, expired, captured, or an invalid capture amount is specified, returns an error."
  ([charge-id params]
   (capture! charge-id {}))
  ([charge-id params opts]
   (h/post-req (str "charges/" charge-id) (assoc opts :params params))))

(s/fdef capture!
        :args (s/cat :charge-id string?
                     :params ::capture-params
                     :opts (s/? h/request-options?))
        :ret (ts/async string?))


(defn fetch-all
  "Returns a channel containing all charges if they exist up to a certain limit, if specified. If charges do not exist, return will be an empty vector."
  ([params]
   (fetch-all {}))
  ([params opts]
   (h/get-req "charges" (assoc opts :params params))))


(s/fdef fetch-all
        :args (s/cat :params ::fetch-all-params
                     :opts (s/? h/request-options?))
        :ret (ts/async ::charges))


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
    (create! 500 {:out-ch c} {:customer    "cus_BzZW6T3NzySJ5E"
                          :description "Test platform charge"})
    c)


  (defn random-function []
    (create! 800 {:customer    "cus_BzZW6T3NzySJ5E"
              :description "Test platform charge"}))

  ;; synchronous
  (h/with-token "sk_test_mPUtCMOnGXJwD6RAWMPou8PH"
    (random-function))

  (random-function)


  (h/with-connect-account "acct_191838JDow24Tc1a"
    (create! 700 {:customer    "cus_BU7S7e46Y0wed9"
              :description "Test connect charge"}))

  (h/with-connect-account "acct_191838JDow24Tc1a"
    (amount-refunded (fetch "py_1BbyfuJDow24Tc1arEHZ7Ecl")))

  )
