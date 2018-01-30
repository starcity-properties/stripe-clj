(ns user
  (:require [clojure.spec.test.alpha :as stest]
            ;; TODO: Add rest of namespaces
            [stripe.account]
            [stripe.bank]
            [stripe.card]
            [stripe.http]))

(stest/instrument)

(defmacro g
  "Turn symbol 'ns' into symbol and go to namespace."
  [ns]
  `(in-ns (symbol (str "stripe." (name '~ns)))))
