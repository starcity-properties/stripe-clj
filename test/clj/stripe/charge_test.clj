(ns stripe.charge-test
  (:require [clojure.test :refer :all]
            [cheshire.core :as json]
            [ring.util.codec :as codec]
            [stripe.charge :as charge]
            [stripe.http :as h]
            [stub-http.core :as stub :refer [with-routes!]]))


(defn api-token-fixture [token]
  (fn [f]
    (h/with-token token
      (f))))

(use-fixtures :once (api-token-fixture "test-token"))


(defn keywordize-keys [m]
  (reduce
   (fn [acc [k v]]
     (->> (if (map? v) (keywordize-keys v) v)
          (assoc acc (keyword k))))
   {}
   m))


(defn stub-handler [request]
  {:status       200
   :content-type "application/json"
   :body         (json/encode request)})


(defn decode-post-body [m]
  (-> (assoc m :body (get-in m [:body :postData]))
      (update :body (comp keywordize-keys codec/form-decode))))


#_(defmacro with-stripe-routes! [route-specs & forms]
    `(with-routes!
       ~(reduce #(assoc %1 %2 stub-handler) {} route-specs)
       (h/with-base-url 'uri
         ~@forms)))



(deftest create-charge-test
  (with-routes!
    {{:method :post :path "/charges"} stub-handler}
    (h/with-base-url (str uri "/")
      (let [{:keys [method path body]} (decode-post-body (charge/create! 100 {}))]
        (is (= "POST" method))
        (is (= "/charges" path))
        (is (= (:currency body) "usd"))))))


(deftest fetch-charge-test
  (with-routes!
    {{:method :get :path "/charges/id"} stub-handler}
    (h/with-base-url (str uri "/")
      (let [{:keys [method path body] :as res} (charge/fetch "id" {})]
        (println res)))))



(deftest create-charge)
;; (is (map? create! 100 {:customer "cus_BzZW6T3NzySJ5E"}))


(deftest dummy-test
  (testing "this test should always pass")
  (is (nil? nil) "succeeds silently")
  (is (nil? "charge") "OK >> designed to fail"))


(comment

  ;; type `, s e` under this expression to run all tests in this namespace
  (run-tests 'stripe.charge-test)

  )
