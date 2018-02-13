(ns stripe.token-test
  (:require [stripe.http :as h]
            [stripe.test-data :as td]
            [stripe.token :as tk]
            [clojure.spec.test.alpha :as stest]
            [clojure.test :refer :all]))

(deftest roundtrip-card-token-test
  "Creating a card and looking it up by its id produce the same card."
  (let [card-token (tk/create-card-token! td/test-card)]
    (is (= (:body card-token) (:body (tk/get-token (:id card-token)))))))

(deftest roundtrip-bank-token-test
  "Creating a bank account and looking it up by its id produces the same bank account."
  (let [bank-token (tk/create-bank-token! td/test-bank)]
    (println (:body bank-token))
    (is (= (:body bank-token) (:body (tk/get-token (:id bank-token)))))))


(comment

  (run-tests 'stripe.token-test)

  )
