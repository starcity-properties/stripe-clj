(ns stripe.invoice
  (:require [clojure.spec.alpha :as s]
            [stripe.http :as h]
            [stripe.spec :as ss]))


;; ==========================================================================
;; spec =====================================================================
;; ==========================================================================


;; invoice ===================================================================

(s/def ::id
  string?)

(s/def ::amount_due
  integer?)

(s/def ::application_fee
  integer?)

(s/def ::attempt_count
  (s/or pos-int? zero?))

(s/def ::attempted
  boolean?)

(s/def ::billing
  #{"charge_automatically" "send_invoice"})

(s/def ::charge
  string?)

(s/def ::closed
  boolean?)

(s/def ::currency
  ss/currency?)

(s/def ::customer
  string)

(s/def ::date
  ss/timestamp-query?)

(s/def ::description
  string?)

(s/def ::discount
  map?)                                 ;; discount object

(s/def ::due_date
  (s/nilable ss/timestamp-query?))

(s/def ::ending_balance
  (s/nilable integer?))

(s/def ::forgiven
  boolean?)

(s/def ::lines
  vector?)                                ;; child

(s/def ::livemode
  ss/livemode?)

(s/def ::next_payment_attempt
  (s/nilable ss/timestamp-query?))

(s/def ::number
  string?)

(s/def ::paid
  boolean?)

(s/def ::period_end
  ss/timestamp-query?)

(s/def ::period_start
  ss/timestamp-query?)

(s/def ::receipt_number
  string?)

(s/def ::starting_balance
  integer?)

(s/def ::statement_descriptor
  ss/statement-descriptor?)

(s/def ::subscription
  (s/nilable string?))

(s/def ::subscription_proration_date
  integer?)

(s/def ::subtotal
  integer?)

(s/def ::tax
  (s/nilable integer?))

(s/def ::tax_percent
  (s/nilable decimal?))

(s/def ::total
  integer?)

(s/def ::webhooks_delivered_at
  ss/timestamp-query?)

(s/def ::invoice
  (-> (s/keys :req-un [::id ::amount_due ::application_fee ::attempt_count ::attempted
                       ::billing ::charge ::closed ::currency ::customer ::date ::description
                       ::discount ::due_date ::ending_balance ::forgiven ::lines ::livemode
                       ::next_payment_attempt ::number ::paid ::period_end ::period_start
                       ::receipt_number ::starting_balance ::statement_descriptor ::subscription
                       ::subtotal ::tax ::tax_percent ::total ::webhooks_delivered_at]
              :opt-un [::subscription_proration_date])
      (ss/metadata)
      (ss/stripe-object "invoice")))

(s/def ::invoices
  (ss/sublist ::invoice))


;; line_item ===================================================================

(s/def ::amount
  integer?)

(s/def ::discountable
  boolean?)

(s/def ::period
  map?)

(s/def ::plan
  (s/nilable map?))                              ;; plan object

(s/def ::proration
  boolean?)

(s/def ::quantity
  (s/nilable integer?))

(s/def ::subscription_item
  string?)

(s/def ::type
  #{"invoiceitem" "subscription"})

(s/def ::line_item
  (-> (s/keys :req-un [::id ::amount ::currency ::description ::discountable ::livemode
                       ::period ::plan ::proration ::quantity ::subscription ::type]
              :opt-un [::subscription_item])
      (ss/metadata)
      (ss/stripe-object "line_item")))


;; create-params ============================================================

(s/def ::days_until_due
  integer?)

(s/def ::due_date
  ss/timestamp-query?)

(s/def ::create-params
  (-> (s/keys :opt-un [::application_fee ::billing ::days_until_due ::description ::due_date
                       ::statement_descriptor ::subscription ::tax_percent])
      (ss/metadata)))


;; fetch-all-params =========================================================

(s/def ::fetch-all-params
  (-> (s/keys :opt-un [::application_fee ::billing ::days_until_due ::description ::due_date
                       ::statement_descriptor ::subscription ::tax_percent])
      (ss/metadata)))


;; fetch-line-item-params ===================================================

;; TODO
;; (s/def ::coupon) preview applying coupon to invoice if subscription or subscription_items is provided

(s/def ::ending_before
  string?)

(s/def ::limit
  integer?)

(s/def ::starting_after
  string?)

(s/def ::subscription_items
  (ss/sublist ::subscription))                     ;; TODO has child arguments to update subscription, presumably same as noted in subscription object spec

(s/def ::subscription_prorate
  boolean?)

(s/def ::subscription_tax_percent
  decimal?)

(s/def ::subscription_trial_end
  ss/timestamp-query?)

(s/def ::fetch-line-item-params
  (-> (s/keys :opt-un [::coupon ::customer ::ending_before ::limit ::starting_after ::subscription
                       ::subscription_items ::subscription_prorate ::subscription_proration_date
                       ::subscription_tax_percent ::subscription_trial_end])
      (ss/metadata)))


;; fetch-upcoming-params ====================================================

(s/def ::invoice_items
  (ss/sublist ::invoice))                        ;; TODO has child arguments to update invoice, presumably same as noted in invoice object spec

(s/def ::fetch-upcoming-params
  (-> (s/keys :opt-un [::coupon ::customer ::invoice_items ::subscription ::subscription_items
                       ::subscription_prorate ::subscription_proration_date ::subscription_tax_percent
                       ::subscription_trial_end])
      (ss/metadata)))


;; update-params ============================================================

(s/def ::update-params
  (-> (s/keys :opt-un [::application_fee ::closed ::description ::forgiven ::paid
                       ::statement_descriptor ::tax_percent])
      (ss/metadata)))


;; pay-params ===============================================================

(s/def ::source
  string?)

(s/def ::pay-params
  (-> (s/keys :opt-un [::source])
      (ss/metadata)))


;; ==========================================================================
;; http api =================================================================
;; ==========================================================================


(defn create!
  "Create an invoice."
  ([customer-id]
   (create! customer-id {} {}))
  ([customer-id params]
   (create! customer-id params {}))
  ([customer-id params opts]
   (let [params (assoc params :customer customer-id)]
   (h/post-req "invoices" (assoc opts :params params)))))

(s/fdef create!
        :args (s/alt :unary (s/cat :customer-id ::customer)
                     :binary (s/cat :customer-id ::customer
                                    :params ::create-params)
                     :ternary (s/cat :customer-id ::customer
                                     :params ::create-params
                                     :opts (s/? h/request-options?)))
        :ret (ss/async ::invoice))


(defn fetch
  "Fetch an invoice."
  ([invoice-id]
   (fetch invoice-id {}))
  ([invoice-id opts]
   (h/get-req (str "invoices/" invoice-id) opts)))

(s/fdef fetch
        :args (s/cat :invoice-id ::id
                     :opts h/request-options?)
        :ret (ss/async ::invoice))


(defn fetch-all
  "Fetch multiple invoices."
  ([]
   (fetch-all {} {}))
  ([params]
   (fetch-all params {}))
  ([params opts]
   (h/get-req "invoices" (assoc opts :params params))))

(s/fdef fetch-all
        :args (s/alt :nullary (s/cat)
                     :unary (s/cat :params ::fetch-all-params)
                     :binary (s/cat :params ::fetch-all-params
                                    :opts h/request-options?))
        :ret (ss/async ::invoices))


(defn fetch-line-items
  "Fetch an invoice's line items."
  ([invoice-id]
   (fetch-line-items invoice-id {} {}))
  ([invoice-id params]
   (fetch-line-items invoice-id params {}))
  ([invoice-id params opts]
   (h/get-req (format "invoices/%s/lines" invoice-id)
              (assoc opts :params params))))

(s/fdef fetch-line-items
        :args (s/alt :unary (s/cat :invoice-id ::id)
                     :binary (s/cat :invoice-id ::id
                                    :params ::fetch-line-item-params)
                     :ternary (s/cat :invoice-id ::id
                                     :params ::fetch-line-item-params
                                     :opts h/request-options?))
        :ret (ss/async ::invoices))


(defn fetch-upcoming
  "Fetch an upcoming invoice, per customer specified."
  ([customer-id]
   (fetch-upcoming customer-id {} {}))
  ([customer-id params]
   (fetch-upcoming customer-id params {}))
  ([customer-id params opts]
   (let [params (assoc params :customer-id customer-id)]
     (h/get-req "invoices/upcoming" (assoc opts :params params)))))

(s/fdef fetch-upcoming
        :args (s/alt :unary (s/cat :customer-id ::customer)
                     :binary (s/cat :customer-id ::customer
                                    :params ::fetch-upcoming-params)
                     :ternary (s/cat :customer-id ::customer
                                     :params ::fetch-upcoming-params
                                     :opts h/request-options?))
        :ret (ss/async ::invoice))


(defn update!
  "Update an invoice, such as to close it."
  ([invoice-id]
   (update! invoice-id {} {}))
  ([invoice-id params]
   (update! invoice-id params {}))
  ([invoice-id params opts]
   (h/post-req (str "invoices/" invoice-id) (assoc opts :params params))))

(s/fdef update!
        :args (s/alt :unary (s/cat :invoice-id ::id)
                     :binary (s/cat :invoice-id ::id
                                    :params ::update-params)
                     :ternary (s/cat :invoice-id ::id
                                     :params ::update-params
                                     :opts h/request-options?))
        :ret (ss/async ::invoice))


(defn pay!
  "Attempt payment on an invoice, outside of normal collection schedule."
  ([invoice-id]
   (pay! invoice-id {} {}))
  ([invoice-id params]
   (pay! invoice-id params {}))
  ([invoice-id params opts]
   (h/post-req (format "invoices/%s/pay" invoice-id) (assoc opts :params params))))

(s/fdef pay!
        :args (s/alt :unary (s/cat :invoice-id ::id)
                     :binary (s/cat :invoice-id ::id
                                    :params ::pay-params)
                     :ternary (s/cat :invoice-id ::id
                                     :params ::pay-params
                                     :opts h/request-options?))
        :ret (ss/async ::invoice))


(comment

)
