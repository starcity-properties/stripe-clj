(ns stripe.spec
  (:require [clojure.spec.alpha :as s]
            [toolbelt.spec]))


;; ;; timestamp ============================


;; (s/def ::gt
;;   ts/unix-timestamp?)

;; (s/def ::gte
;;   ts/unix-timestamp?)

;; (s/def ::lt
;;   ts/unix-timestamp?)

;; (s/def ::lte
;;   ts/unix-timestamp?)

;; (s/def ::timestamp-query
;;   (s/keys :opt-un [::gt ::gte ::lt ::lte]))


;; (defn timestamp-query? [x]
;;   (s/valid? ::timestamp-query x))


;; statement-descriptor =================


(s/def ::statement-descriptor
  (s/and string? #(<= (count %) 22)))

(defn statement-descriptor?
  "Is the argument a valid statement descriptor"
  [x]
  (s/valid? ::statement-descriptor x))


;; stripe-specific ======================


(s/def ::currency
  (s/and string? #(= 3 (count %))))

(defn currency? [x]
  (s/valid? ::currency-id x))


(s/def ::country
  (s/and string? #(= 2 (count %))))

(defn country? [x]
  (s/valid? ::country x))


(s/def ::last4 (s/and string? #(= 4 (count %))))

(defn last4? [x] (s/valid? ::last4 x))


;; errors (stripe) ======================


(s/def ::error map?)

(s/def ::stripe-error
  (s/keys :req-un [::error]))


;; deleted (stripe) =====================


(s/def ::deleted (partial = "true"))

(s/def ::deleted-response
  (s/keys :req-un [::deleted ::id]))

(defn deleted? [x]
  (s/valid? ::deleted-response x))


;; stripe-object ========================


(defn stripe-object
  "Adds an `:object` key to Stripe's return objects."
  [spec object-name]
  (s/and spec (comp (partial = object-name) :object)))


;; sublist (stripe) =====================


(s/def ::has_more boolean?)

(s/def ::url string?)

(s/def ::total_count integer?)

(s/def ::count integer?)

(s/def ::livemode boolean?)

(defn livemode? [x] (s/valid? ::livemode x))

(s/def :sublist/object (partial = "list"))

(defn sublist
  "Returns a spec for a sublist object."
  [data-spec]
  (s/and (s/keys :req-un [:sublist/object ::has_more ::url ::data]
                 :opt-un [::total_count ::count])
         (comp (partial s/valid? (s/* data-spec)) :data)))


;; pagination ===========================


;; (s/def ::limit
;;   (s/and integer? (ts/between 1 101)))

(defn limit? [x]
  (s/valid? ::limit x))


;; metadata =============================


(s/def ::metadata
  (s/and (s/map-of keyword? string?)
         ;; Only 20 KV pairs are currently supported.
         #(< (count %) 20)))

(defn metadata [spec]
  (s/and spec (s/keys :opt-un [::metadata])))

(defn metadata? [m]
  (s/valid? ::metadata m))
