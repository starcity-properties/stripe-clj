(ns stripe.subscription
  (:require [clojure.spec.alpha :as s]
            [stripe.card :as card]
            [stripe.http :as h]
            [stripe.plan :as plan]
            [stripe.subscription-item :as sub-item]
            [stripe.spec :as ss]
            [toolbelt.spec :as ts]))

;; ==============================================================================
;; spec =========================================================================
;; ==============================================================================

(s/def ::id
  string?)

(s/def ::application_fee_percent
  (s/nilable (s/and pos? (ts/between 0 101))))

(s/def ::billing
  #{"charge_automatically" "send_invoice"})

(s/def ::cancel_at_period_end
  boolean?)

(s/def ::canceled_at
  (s/nilable ts/unix-timestamp?))

(s/def ::created
  ts/unix-timestamp?)

(s/def ::current_period_end
  ts/unix-timestamp?)

(s/def ::current_period_start
  ts/unix-timestamp?)

(s/def ::customer
  string?)

(s/def ::days_until_due
  (s/nilable pos-int?))

(s/def ::discount
  (s/nilable map?))

(s/def ::ended_at
  (s/nilable ts/unix-timestamp?))

(s/def ::items
  (ss/sublist sub-item/subscription-item?))

(s/def ::livemode
  ss/livemode?)

(s/def ::plan
  plan/plan?)

(s/def ::quantity
  integer?)

(s/def ::start
  ts/unix-timestamp?)

(s/def ::status
  #{"trailing" "active" "past_due" "canceled" "unpaid"})

(s/def ::tax_percent
  (s/nilable (s/and number? pos?)))

(s/def ::trial_end
  (s/nilable ts/unix-timestamp?))

(s/def ::trial_start
  (s/nilable ts/unix-timestamp?))

(s/def ::billing_cycle_anchor
  (s/nilable ss/unix-timestamp?))

(s/def ::subscription
  (-> (s/keys :req-un [::id ::application_fee_percent ::billing ::cancel_at_period_end
                       ::canceled_at ::created ::current_period_end ::current_period_start
                       ::customer ::days_until_due ::discount ::ended_at ::items ::livemode
                       ::plan ::quantity ::start ::status ::tax_percent ::trial_start
                       ::trial_end ::billing_cycle_anchor])
      (ss/metadata)
      (ss/stripe-object "subscription")))

(s/def ::subscriptions
  (ss/sublist ::subscription))


;; create-params ========================


(s/def ::coupon
  string?)

(s/def ::source
  (s/or :string string? :card card/card?))

(s/def :subscription.subscription-item/plan
  string?)

(s/def ::subscription-item
  (s/keys :req-un [:subscription.subscription-item/plan] :opt-un [::quantity]))

(s/def ::plan-ids
  (s/or :plan string? :items (s/+ ::subscription-item)))

(s/def ::create-params
  (-> (s/keys :opt-un [::application_fee_percent ::billing ::coupon
                       ::days_until_due ::items ::source ::billing_cycle_anchor])
      (ss/metadata)))


;; update-params ========================


(s/def ::prorate
  boolean?)

(s/def ::proration_date
  ts/unix-timestamp?)

(s/def ::update-params
  (-> (s/keys :opt-un [::application_fee_percent ::billing ::coupon ::days_until_due
                       ::items ::prorate ::proration_date ::source ::tax_percent ::trial_end])
      (ss/metadata)))


;; cancel-params ========================


(s/def ::at_period_end
  boolean?)

(s/def ::cancel-params
  (s/keys :opt-un [::at_period_end]))


;; fetch-all-params =====================


(s/def ::ending_before
  string?)

(s/def ::limit
  ss/limit?)

(s/def ::starting_after
  string?)

(s/def fetch-all-status
  #{"trialing" "active" "past_due" "unpaid" "canceled" "all"})

(s/def ::fetch-all-params
  (-> (s/keys :opt-un [::billing ::created ::customer ::ending_before ::limit
                       ::plan ::starting_after ::fetch-all-status])))


;; ==============================================================================
;; http api =====================================================================
;; ==============================================================================


(defn create!
  "Create a subscription."
  ([customer-id plan-ids]
   (create! customer-id plan-ids {} {}))
  ([customer-id params plan-ids]
   (create! customer-id plan-ids params {}))
  ([customer-id plan-ids params opts]
   (let [[k v]   (s/conform ::plan-ids plan-ids)
         params' (assoc params
                        :customer customer-id
                        k v)]
     (h/post-req "subscriptions"
                 (assoc opts :params params')))))

(s/fdef create!
        :args (s/alt :binary (s/cat :customer-id ::customer
                                    :plan-ids ::plan-ids)
                     :ternary (s/cat :customer-id ::customer
                                     :plan-ids ::plan-ids
                                     :params ::create-params)
                     :quaternary (s/cat ::customer-id ::customer
                                        :plan-ids ::plan-ids
                                        :params ::create-params
                                        :opts h/request-options?))
        :ret (ts/async ::subscription))


(defn fetch
  "Fetch a subscription."
  ([subscription-id]
   (fetch subscription-id {}))
  ([subscription-id opts]
   (h/get-req (str "subscriptions/" subscription-id) opts)))

(s/fdef fetch
        :args (s/cat :subscription-id ::id
                     :opts (s/? h/request-options?))
        :ret (ts/async ::subscription))


(defn update!
  "Update a subscription."
  ([subscription-id]
   (update! subscription-id {} {}))
  ([subscription-id params]
   (update! subscription-id params {}))
  ([subscription-id params opts]
   (h/post-req (str "subscriptions/" subscription-id)
               (assoc opts :params params))))

(s/fdef update!
        :args (s/alt :unary (s/cat :subscription-id ::id)
                     :binary (s/cat :subscription-id ::id
                                    :params ::update-params)
                     :ternary (s/cat :subscription-id ::id
                                    :params ::update-params
                                    :opts h/request-options?))
        :ret (ts/async ::subscription))


(defn cancel!
  "Cancels a customer's subscription."
  ([subscription-id]
   (cancel! subscription-id {}))
  ([subscription-id params]
   (cancel! subscription-id params {}))
  ([subscription-id params opts]
   (h/delete-req (str "subscriptions/" subscription-id)
                 (assoc opts :params params))))

(s/fdef cancel!
        :args (s/alt :unary (s/cat :subscription-id ::id)
                     :binary (s/cat :subscription-id ::id
                                    :params ::cancel-params)
                     :ternary (s/cat :subscription-id ::id
                                     :params ::cancel-params
                                     :opts h/request-options?))
        :ret (ts/async ::subscription))

(defn fetch-all
  "Fetches all customer subscriptions."
  ([]
   (fetch-all {} {}))
  ([params]
   (fetch-all params {}))
  ([params opts]
   (h/get-req "subscriptions" (assoc opts :params params))))

(s/fdef fetch-all
        :args (s/alt :nullary (s/cat)
                     :unary (s/cat :params ::fetch-all-params)
                     :binary (s/cat :params ::fetch-all-params
                                    :opts h/request-options?))
        :ret (ts/async ::subscriptions))

(comment

  (do
    (h/use-token! "sk_test_mPUtCMOnGXJwD6RAWMPou8PH")
    (def test-customer "cus_CBaoFshG350779")
    (def test-card {:object "card" :exp_month 8 :exp_year 2021 :number "4242424242424242" :cvc 242})
    ;; (def test-plan (:id (plan/create! "some-plan name" "year" 1199)))
    ;; (def test-plan2 (:id (plan/create! "another-plan name" "year" 1199)))
    )

  (do
    (require '[clojure.spec.test.alpha :as stest])
    (stest/instrument))

  (create! test-customer test-plan)
  (create! test-customer [test-plan test-plan2])

  (s/explain ::subscription (fetch "sub_CGY5NNR70V2TYj"))

  (update! "sub_CGY5NNR70V2TYj" {:tax_percent 3})

  (cancel! "sub_CGY5NNR70V2TYj" {:at_period_end true})

  (fetch-all)

  )
