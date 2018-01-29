(ns stripe.connect
  (:require [clojure.spec.alpha :as s]
            [stripe.http :as h]
            [stripe.spec :as ss]))


;; ==========================================================================
;; spec =====================================================================
;; ==========================================================================


;; connect ==================================================================

(s/def ::id
  string?)

(s/def ::amount_due
  integer?)

(s/def ::application_fee
  integer?) bb

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



;; fetch-params =============================================================



;; fetch-all-params =========================================================



;; fetch-line-item-params ===================================================



;; fetch-upcoming-params ====================================================



;; update-params ============================================================



;; pay-params ===============================================================



;; ==========================================================================
;; http api =================================================================
;; ==========================================================================


(defn create!
  )

(s/fdef create!
        :args
        :ret)


(defn fetch
  )

(s/fdef fetch
        :args
        :ret)


(defn fetch-all
  )

(s/fdef fetch-all
        :args
        :ret)


(defn fetch-line-items
  )

(s/fdef fetch-line-items
        :args
        :ret)


(defn fetch-upcoming
  )

(s/fdef fetch-upcoming
        :args
        :ret)


(defn update!
  )

(s/fdef update!
        :args
        :ret)


(defn pay!
  )

(s/fdef pay!
        :args
        :ret)
