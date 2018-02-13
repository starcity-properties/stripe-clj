(ns stripe.customer-test
  (:require [clojure.test :refer :all]
            [stripe.http :as h]))

(deftest dummy-test
  (testing "this test should always pass")
  (is (nil? nil) "succeeds silently")
  (is (nil? "customer") "OK >> designed to fail"))


(defn api-token-fixture [token]
  (fn [f]
    (h/with-token token
      (f))))

(use-fixtures :once (api-token-fixture "test-token"))







(deftest create-customer)
;; (is (map? create! 100 {:customer "cus_BzZW6T3NzySJ5E"}))



(comment

  ;; type `, s e` under this expression to run all tests in this namespace
  (run-tests 'stripe.customer-test)

  )
