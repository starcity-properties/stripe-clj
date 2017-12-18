(ns stripe.util
  "Helpers for stripe-clj's schema definitions.")

(defn between
  "returns a predicate that checks that the supplied number falls
  between the inclusive lower and exclusive upper bounds supplied."
  [low high]
  (fn [x]
    (and (>= x low)
         (< x high))))

(defn collectify [x]
  #?(:cljs
     (if (sequential? x) x [x])
     :clj
     (cond (nil? x) []
           (or (sequential? x) (instance? java.util.List x) (set? x)) x
           :else [x])))
