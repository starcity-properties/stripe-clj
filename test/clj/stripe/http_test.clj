(ns stripe.http-test
  (:require [stripe.http :as h]
            [clojure.spec.test.alpha :as stest]
            [clojure.test :refer :all]))


(deftest api-token-test

  (is (nil? (h/api-token))
      "api token is unbound by default")

  (h/with-token "something"
    (is (= (h/api-token) "something")
        "token can be bound on a per-thread basis")

    (h/with-token "something-else"
      (is (= (h/api-token) "something-else")
          "the token can be overridden by nesting `with-token`"))

    #_(testing "can globally modify the api token"

        (h/use-token! "global")
        (is (= "global" (h/api-token)))
        (h/use-token! nil)
        (is (nil? (h/api-token)))
        ))


  )


(deftest method-url-test
  (is (= (h/method-url "") h/*url*)
      "the base url is set by default")
  (is (= (h/method-url "hello") "https://api.stripe.com/v1/hello")
      "adds `hello` to the base url")

  (testing "the base url can be overridden"
    (h/with-base-url "https://localhost/"
      (is (= (h/method-url "hello") "https://localhost/hello")))

    (is (= (h/method-url "") "https://api.stripe.com/v1/")
        "the base url is unchanged outside of `h/with-base-url`")))



;; TODO: test `prepare-params`
