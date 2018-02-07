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

(defprotocol ^:no-doc FormEncodeable
  (form-encode* [this encoding]))

(extend-protocol FormEncodeable
  String
  (form-encode* [unencoded encoding]
    (URLEncoder/encode unencoded encoding))
  Map
  (form-encode* [params encoding]
    (letfn [(flatten-keys [ps]
              (map #(str "[" (encode (name %)) "]") ps))
            (parameterize [[p & ps]]
              (apply str (encode (name p)) (flatten-keys ps)))
            (parameterize-index [[p & ps] index]
              (apply str (encode (name p)) "[" index "]" (flatten-keys ps)))
            (encode [x]
              (form-encode* x encoding))
            (encode-array-param [index k v]
              (if (map? v)
                (let [params (->> (flatten-map v) (map (partial cons k)))]
                  (->> params
                       (map
                        (fn [param]
                          (let [value (encode (last param))
                                name  (parameterize-index (butlast param) index)]
                            (if (nil? value)
                              (str name "=")
                              (str name "=" value)))))
                       (str/join "&")))
                (str (encode (name k)) "[]=" (encode v))))
            (encode-map-params [[k v]]
              (let [params (->> (flatten-map v) (map (partial cons k)))]
                (map (fn [param]
                       (let [value (encode (last param))
                             name  (parameterize (butlast param))]
                         (cond
                           (nil? value)        (str name "=")
                           (sequential? value) (str name "[]=" value)
                           :otherwise          (str name "=" value))))
                     params)))
            (encode-param [[k v]]
              (if (nil? v)
                (str (encode (name k)) "=")
                (str (encode (name k)) "=" (encode v))))]
      (->> params
           (mapcat
            (fn [[k v]]
              (cond
                (or (set? v) (sequential? v)) (map-indexed #(encode-array-param %1 k %2) v)
                (map? v)                      (encode-map-params [k v])
                :otherwise                    [(encode-param [k v])])))
           (str/join "&"))))
  Object
  (form-encode* [x encoding]
    (form-encode* (str x) encoding))
  nil
  (form-encode* [x encoding] ""))

(defn form-encode
  "Encode the supplied value into www-form-urlencoded format, often used in
  URL query strings and POST request bodies, using the specified encoding.
  If the encoding is not specified, it defaults to UTF-8"
  [x & [encoding]]
  (form-encode* x (or encoding "UTF-8")))
