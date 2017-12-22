(ns stripe.account
  "Functions for Stripe's account API."
  (:require [clojure.spec.alpha :as s]
            [stripe.http :as h]
            [toolbelt.async :as ta]
            [stripe.schema :as ss]))

(s/def ::account map?)


(defn account
  "Retrieves the details of the account, based on the API key that was
  used to make the request."
  ([] (account {}))
  ([opts]
   (h/get-req "account" opts)))

(s/fdef account
        :args (s/cat :opts (s/? :stripe.http/request-options))
        :ret (ss/async ::account))
