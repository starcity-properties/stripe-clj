(ns stripe.test-fixtures
  "Performs test setup and teardown around Stripe API."
  (:require [environ.core :as e]
            [stripe.customer :as c]
            [stripe.http :as h]))


;; ## Helpers for Testing

(defmacro with-customer
  "Synchronously creates a customer and binds it to `sym` for the body
  of the test. The macro deletes the customer after the body runs, but
  still returns the last form of the supplied body."
  [[sym data] & body]
  `(let [~sym (c/create-customer ~data)]
     (try ~@body
          (finally (c/delete-customer (:id ~sym))))))


(defn env-token
  "Clojure.test fixture that sets the stripe token for all tests using
  the environment variable linked to the supplied keyword.
  Use like: (clojure.test/use-fixtures :once (t/env-token :stripe-dev-token))"
  [k]
  (fn [test-fn]
    (h/with-token (k e/env)
      (test-fn))))


(defn api-token-fixture [token]
  (fn [f]
    (h/with-token token
      (f))))


(defn api-synchronous-fixture
  [f]
  (h/with-token (env-token :stripe-dev-token)))

(defn test-setup
  "Wrapper function for use with use-fixtures."
  [f]
  (with-redefs [sptfy/json-string-to-map util/test-json-string-to-map]
    (f)))

(use-fixtures :each test-setup)

;; register my-test-fixture, which is called once and wraps all tests
(use-fixtures :once (api-token-fixture "test-token"))

(use-fixtures :once (env-token :stripe-dev-token))

;; register another-fixture, which is called for each test wrapped
(use-fixtures :each another-fixture)


;; (create! {:customer    "cus_BzZW6T3NzySJ5E"
;;           :description "Test create customer"}
;;          {:token (config/stripe-private-key config)})


(defn generate-test
  "Create unit test code, keeping it DRY."
  [api-call infile]
  (let [correct-test-data (util/test-json-string-to-map (slurp infile))
        differences (data/diff api-call correct-test-data)]
    (is (= nil (first differences) (second differences)))))
