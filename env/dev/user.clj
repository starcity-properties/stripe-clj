(ns user
  (:require [clojure.spec.test.alpha :as stest]
            [stripe.account]
            [stripe.balance]
            [stripe.bank]
            [stripe.card]
            [stripe.charge]
            [stripe.customer]
            [stripe.event]
            [stripe.http]
            [stripe.invoice]
            [stripe.payout]
            [stripe.plan]
            [stripe.refund]
            [stripe.spec]
            [stripe.token]
            [stripe.transfer]))

(stest/instrument)


(defmacro g
  "Turn symbol 'ns' into symbol and go to namespace."
  [ns]
  `(in-ns (symbol (str "stripe." (name '~ns)))))
