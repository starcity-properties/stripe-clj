(ns stripe.bank
  (:require [clojure.spec.alpha :as s]
            [stripe.spec :as ss]
            [stripe.http :as h]
            [stripe.util :as u]))

(s/def ::id
  string?)

(s/def ::account
  string?)

(s/def ::account_holder_name
  string?)

(s/def ::account_holder_type
  string?)

(s/def ::bank_name
  string?)

(s/def ::country
  string?)

(s/def ::currency
  ss/currency?)

(s/def ::default_for_currency
  boolean?)

(s/def ::fingerprint
  string?)

(s/def ::bank_account
  (-> (s/keys :req-un)
      (ss/stripe-object "bank_account")))
