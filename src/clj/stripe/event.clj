(ns stripe.event
  (:require [clojure.spec.alpha :as s]
            [stripe.http :as h]
            [stripe.spec :as ss]
            [toolbelt.spec :as ts]))

(s/def ::id
  string?)

(s/def ::api_version
  string?)

(s/def ::created
  ts/unix-timestamp?)

(s/def ::object
  map?)

(s/def ::previous_attributes
  map?)

(s/def ::data
  (s/keys :req-un [::object]
          :opt-un [::previous_attributes]))

(s/def ::livemode
  boolean?)

(s/def ::pending_webhooks
  (s/or :zero zero?
        :positive pos-int?))

(s/def :stripe.event.request/id
  (s/nilable string?))

(s/def ::idempotency_key
  (s/nilable string?))

(s/def ::request
  (s/keys :req-un [:stripe.event.request/id ::idempotency_key]))

(s/def ::type
  string?)

(s/def ::event
  (-> (s/keys :req-un [::id ::api_version ::created ::data ::livemode ::pending_webhooks ::request ::type])
      (ss/stripe-object "event")))

(s/def ::events
  (ss/sublist ::event))

(s/def :stripe.event.fetch-all/created
  ss/timestamp-query?)

(s/def ::ending_before
  string?)

(s/def ::limit
  ss/limit?)

(s/def ::starting_after
  string?)

(s/def ::types
  (s/+ ::type))

(s/def ::fetch-all-params
  (s/keys :opt-un [:stripe.event.fetch-all/created ::ending_before ::limit
                   ::starting_after ::type ::types]))


(defn fetch
  ([event-id]
   (fetch event-id {}))
  ([event-id opts]
   (h/get-req (str "events/" event-id) opts)))

(s/fdef fetch
        :args (s/cat :event-id ::id
                     :opts (s/? h/request-options?))
        :ret (ts/async ::event))


(defn fetch-all
  ([]
   (fetch-all {} {}))
  ([params]
   (fetch-all params {}))
  ([params opts]
   (h/get-req "events" (assoc opts :params params))))

(s/fdef fetch-all
        :args (s/alt :nullary (s/cat)
                     :unary (s/cat :params ::fetch-all-params)
                     :binary (s/cat :params ::fetch-all-params
                                    :opts h/request-options?))
        :ret (ts/async ::events))


(comment

  (h/use-token! "sk_test_mPUtCMOnGXJwD6RAWMPou8PH")

  (do
    (require '[clojure.spec.test.alpha :as stest])
    (stest/instrument))

  (fetch-all)

  (every? (comp #{"charge.succeeded" "charge.failed"} :type) (:data (fetch-all {:types ["charge.succeeded" "charge.failed"]})))

  (fetch "evt_1BlpEdIvRccmW9nO6O0EMLf1")

  )
