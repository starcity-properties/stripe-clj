(ns stripe.http
  (:require [cheshire.core :as json]
            [clojure.core.async :as a]
            [clojure.spec.alpha :as s]
            [org.httpkit.client :as http]
            [stripe.spec :as ss]
            [stripe.util :as u]
            [stripe.util.codec :as codec]
            [toolbelt.async :as ta]
            [toolbelt.core :as tb]))

;; =============================================================================
;; Spec
;; =============================================================================


(s/def ::api-token
  string?)

(s/def ::expansion
  (s/or :keyword keyword? :keyseq (s/+ keyword?)))

(s/def ::http-kit-options
  map?)

(s/def ::params
  map?)

(s/def ::method
  #{http/post http/get http/delete})

(s/def ::out-ch
  ta/chan?)

(s/def ::client-options
  ::http-kit-options)

(s/def ::token
  ::api-token)

(s/def ::account
  string?)

(s/def ::request-options
  (s/keys :opt-un [::account ::out-ch ::params ::client-options ::token]))

(s/def ::endpoint
  string?)

(s/def ::api-call
  (s/and (s/keys :req-un [::method ::endpoint])
         ::request-options))


(defn request-options?
  "Is the argument a valid request options map?"
  [x]
  (s/valid? ::request-options x))


(def api-call?
  "Is the argument a valid api call map?"
  (partial s/valid? ::api-call))


;; =============================================================================
;; Authorization
;; =============================================================================


(def ^:dynamic *token* nil)
(def ^:dynamic *api-version* nil)
(def ^:dynamic *connect-account* nil)


;; =====================================
;; API Token


(defn api-token [] *token*)

(s/fdef api-token
        :ret (s/nilable ::api-token))


(defmacro with-token [t & forms]
  `(binding [*token* ~t]
     ~@forms))

(s/fdef with-token
        :args (s/cat :token (s/or :symbol symbol? :token ::api-token)
                     :forms (s/* list?))
        :ret list?)


(defn use-token!
  "Permanently sets a base token. The token can still be overridden on
  a per-thread basis using with-token."
  [t]
  (alter-var-root #'*token* (constantly t)))


;; =====================================
;; API Version


(defn api-version [] *api-version*)

(s/fdef api-version
        :args (s/cat)
        :ret (s/nilable string?))


(defmacro with-api-version
  [v & forms]
  `(binding [*api-version* ~v]
     ~@forms))

(s/fdef with-api-version
        :args (s/cat :version string?
                     :forms (s/* list?))
        :ret list?)


(defn use-api-version!
  "Permanently sets an API version. The api version can still be
  overridden on a per-thread basis using with-api-version."
  [s]
  (alter-var-root #'*api-version* (constantly s)))


;; =====================================
;; Connect Account


(defn connect-account [] *connect-account*)

(s/fdef connect-account
        :ret (s/nilable string?))


(defmacro with-connect-account
  [v & forms]
  `(binding [*connect-account* ~v]
     ~@forms))

(s/fdef with-connect-account
        :args (s/cat :version string?
                     :forms (s/* list?))
        :ret list?)


(defn use-connect-account!
  "Permanently sets an API version. The api version can still be
  overridden on a per-thread basis using with-connect-account."
  [s]
  (alter-var-root #'*connect-account* (constantly s)))


;; =============================================================================
;; Private
;; =============================================================================


(def ^:dynamic *url* "https://api.stripe.com/v1/")


(defmacro with-base-url
  [u & forms]
  `(binding [*url* ~u]
     ~@forms))


(defn method-url
  "URL for calling a method."
  [method]
  (str *url* method))

(s/fdef method-url
        :args (s/cat :method string?)
        :ret string?)


(defn- encode-params
  [method params]
  (case method
    :get [:query-params params]
    [:body (codec/form-encode params)]))


(defn prepare-params
  "Returns a parameter map suitable for feeding in to a request to Stripe.

  `opts` is a set of options for http-kit's client. These kick out the
  defaults.

  `params` is the parameters for the stripe API calls."
  [token method params opts]
  (let [[k params'] (encode-params method params)
        base-params {:basic-auth       token
                     :throw-exceptions false
                     k                 params'}
        version     (or (:api-version opts) (api-version))
        connect     (or (:account opts) (connect-account))
        headers     (tb/assoc-when {} "Stripe-Version" version "Stripe-Account" connect)]
    (merge base-params {:headers headers} (dissoc opts :api-version :account))))

(s/fdef prepare-params
        :args (s/cat :token ::api-token
                     :method ::method
                     :params ::params
                     :opts ::http-kit-options)
        :ret map?)

;; =============================================================================
;; Public API
;; =============================================================================


(defn api-call
  "Call an API method on Stripe. If an output channel is supplied, the
  method will place the result in that channel; if not, returns
  synchronously."
  [{:keys [params client-options token account method endpoint out-ch]
    :or   {params         {}
           client-options {}
           account        *connect-account*
           token          (api-token)}}]
  (assert token "API Token must not be nil.")
  (let [url     (method-url endpoint)
        params' (->> (assoc client-options :account account)
                     (prepare-params token method params))
        process (fn [ret]
                  (or (json/parse-string (:body ret) keyword)
                      {:error (:error ret)}))]
    (if-not (some? out-ch)
      (process @(method url params'))
      (do (method url params'
                  (fn [ret]
                    (a/put! out-ch (process ret))
                    (a/close! out-ch)))
          out-ch))))

(s/fdef api-call
        :args (s/cat :params ::api-call)
        :ret (s/or :result map? :chan ta/chan?))


(defmacro defapi
  "Generates a synchronous and async version of the same function."
  [sym method]
  `(defn ~sym
     ([endpoint#]
      (~sym endpoint# {}))
     ([endpoint# opts#]
      (api-call
       (assoc opts#
              :method ~method
              :endpoint endpoint#)))))

(s/fdef defapi
        :args (s/cat :symbol symbol? :method symbol?)
        :ret list?)

(defapi post-req http/post)
(defapi get-req http/get)
(defapi delete-req http/delete)
