(ns stripe.customer-test
<<<<<<< HEAD
  ;; (:use clojure.test
  ;;       stripe.customer)
  ;; (:require [stripe.test-data :as td])
  )

;; (deftest customer-cycle-test
;;   "Test of the create, update, delete cycle for customers."
;;   (let [created (create-customer td/customer-data)
;;         id (:id created)
;;         retrieved (get-customer id)]
;;     (is (= created retrieved)
;;         "Stripe returns the new customer on creation. Retrieving it
;;          using get should return the same document.")
;;     (let [updated (update-customer id {:metadata {:brandon "lowden"}})]
;;       (is (= updated (assoc-in retrieved [:metadata :brandon] "lowden"))
;;           "Updating a document doesn't touch existing fields; new
;;           supplied fields are merged in to the old customer."))
;;     (is (= {:deleted true :id id} (delete-customer id))
;;         "Deleting a customer returns the id.")
;;     (is (= {:error {:type "invalid_request_error"
;;                     :message (str "No such customer: " id)
;;                     :param "id"}}
;;            (delete-customer id))
;;         "Deleting a customer AGAIN returns an error. (This test may be
;;         too brittle. We'll see if the API string changes.)")))
=======
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
>>>>>>> e008aa5... add test files for charge and customer
