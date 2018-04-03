(ns stripe.async-test
  "Test of Stripe's async capabilities."
  (:use clojure.test)
  (:require [clojure.core.async :as a]
            [stripe.balance :as b]
            [stripe.http :as h]))

(defn api-token-fixture [token]
  (fn [f]
    (h/with-token token
      (f))))

(use-fixtures :once (api-token-fixture "test-token"))

#_(deftest async-test
  (is (= (a/<!! (b/get-balance {:out-ch (a/chan)}))
         (b/get-balance))
      "Supplying an output channel forces the client to stick the
      result into the channel and return that instead of returning the
      value directly."))
