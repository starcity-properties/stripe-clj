(ns stripe.token-test
  (:require [stripe.test-data :as td]
            [stripe.token :as tk]
            [clojure.spec.test.alpha :as stest]
            [clojure.test :refer :all]))

(deftest roundtrip-card-token-test
  (let [card-token (tk/create-card-token! td/test-card)]
    (is (= card-token (tk/get-token (:id card-token)))
        "Creating a card and looking it up by its ID produce the same
        card.")))

(deftest roundtrip-bank-token-test
  (let [bank-token (tk/create-bank-token! td/test-bank)]
    (is (= bank-token (tk/get-token (:id bank-token)))
        "Creating a bank account and looking it up by its ID produces
        the same account.")))
