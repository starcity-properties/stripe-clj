(ns stripe.subscription-item
  (:require [clojure.spec.alpha :as s]
            [stripe.spec :as ss]
            [stripe.plan :as plan]))

;; ==============================================================================
;; spec =========================================================================
;; ==============================================================================

(s/def ::id
  string?)

(s/def ::created
  ss/unix-timestamp?)

(s/def ::metadata
  ss/metadata?)

(s/def ::plan
  plan/plan?)

(s/def ::quantity
  pos-int?)

(s/def ::subscription
  string?)

(s/def ::subscription-item
  (-> (s/keys :req-un [::id ::created ::plan ::quantity ::subscription])
      (ss/metadata)
      (ss/stripe-object "subscription_item")))

(def subscription-item?
  (partial s/valid? ::subscription-item))
