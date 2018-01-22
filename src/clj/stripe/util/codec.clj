(ns stripe.util.codec
  "Both `http-kit` and `ring.util.codec/form-encode` use an invalid (according
  to some, Stripe included) way of form-encoding arrays. This ns is copy of the
  aforementioned Ring implementation with a minor modification -- see line(s)
  marked as such below.

  see also:
  http://stackoverflow.com/questions/6243051/how-to-pass-an-array-within-a-query-string"
  (:require [clojure.string :as str])
  (:import java.util.Map
           [java.net URLEncoder]))

(defn- flatten-map [m]
  (if-not (map? m)
    [[m]]
    (reduce
     (fn [acc [k v]]
       (let [nested (flatten-map v)]
         (vec (concat acc (map (comp vec (partial concat [k])) nested)))))
     []
     m)))

(defn- parameterize [[p & ps]]
  (apply str (name p) (map #(str "[" (name %) "]") ps)))

(defprotocol ^:no-doc FormEncodeable
  (form-encode* [x encoding]))

(extend-protocol FormEncodeable
  String
  (form-encode* [unencoded encoding]
    (URLEncoder/encode unencoded encoding))
  Map
  (form-encode* [params encoding]
    (letfn [(encode [x] (form-encode* x encoding))
            (encode-array-param [[k v]] (str (encode (name k)) "[]=" (encode v)))
            (encode-map-params [[k v]] (let [params (->> (flatten-map v) (map (partial cons k)))]
                                         (map (fn [param]
                                                (let [value (last param)
                                                      name  (parameterize (butlast param))]
                                                  (if (sequential? value)
                                                    (str name "[]=" value)
                                                    (str name "=" value))))
                                              params)))
            (encode-param [[k v]] (str (encode (name k)) "=" (encode v)))]
      (->> params
           (mapcat
            (fn [[k v]]
              (cond
                (or (seq? v) (sequential? v)) (map #(encode-array-param [k %]) v)
                (map? v)                      (encode-map-params [k v])
                :otherwise                    [(encode-param [k v])])))
           (str/join "&"))))
  Object
  (form-encode* [x encoding]
    (form-encode* (str x) encoding)))

(defn form-encode
  "Encode the supplied value into www-form-urlencoded format, often used in
  URL query strings and POST request bodies, using the specified encoding.
  If the encoding is not specified, it defaults to UTF-8"
  [x & [encoding]]
  (form-encode* x (or encoding "UTF-8")))
