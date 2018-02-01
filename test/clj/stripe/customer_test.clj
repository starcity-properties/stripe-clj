(ns stripe.customer-test
  (:require [stripe.http :as h]
            [clojure.spec.test.alpha :as stest]
            [clojure.test :refer :all]))


(deftest dummy-test
  (testing "this test should always pass")
  (is (nil? nil) "succeeds silently")
    (is (nil? "customer") "OK >> designed to fail"))


(deftest create-customer)
;; (is (map? create! 100 {:customer "cus_BzZW6T3NzySJ5E"}))



(comment

  ;; type `, s e` under this expression to run all tests in this namespace
  (run-tests 'stripe.customer-test)

  )
