(ns stripe.charge-test
  (:require [clojure.spec.test.alpha :as stest]
            [clojure.test :refer :all]
            [ring.util.codec :refer [form-decode]]
            [stripe.charge :as charge]
            [stub-http.core :as stub :refer [with-routes!]]
            [stripe.http :as h]
            [cheshire.core :as json]))


(defn api-token-fixture [token]
  (fn [f]
    (h/with-token token
      (f))))


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
      (update :body (comp keywordize-keys form-decode))))


#_(defmacro with-stripe-routes! [route-specs & forms]
    `(with-routes!
       ~(reduce #(assoc %1 %2 stub-handler) {} route-specs)
       (h/with-base-url 'uri
         ~@forms)))


(use-fixtures :once (api-token-fixture "test-token"))


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
