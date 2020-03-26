(ns stripe.account
  "Functions for Stripe's account API."
  (:require [clojure.spec.alpha :as s]
            [stripe.bank :as b]
            [stripe.card :as c]
            [stripe.http :as h]
            [stripe.spec :as ss]
            [toolbelt.spec :as ts]))


;; =============================================================================
;; spec ========================================================================
;; =============================================================================


;; account =====================================================================


(s/def ::id
  string?)

(s/def ::business_name
  string?)

(s/def ::business_url
  string?)                              ;; url

(s/def ::charges_enabled
  boolean?)

(s/def ::country
  ss/country?)

(s/def ::created
  ts/unix-timestamp?)

(s/def ::debit_negative_balances
  boolean?)

(s/def ::avs_failure
  boolean?)

(s/def ::cvc_failure
  boolean?)

(s/def ::decline_charge_on
  (s/keys :req-un [::avs_failure ::cvc_failure]))

(s/def ::default_currency
  ss/currency?)

(s/def ::details_submitted
  boolean?)

(s/def ::display_name
  string?)

(s/def ::email
  string?)                             ;; email address

(s/def ::external-account
  (s/or :bank b/bank? :card c/card?))

(s/def ::external_accounts
  (ss/sublist ::external-account))

(s/def ::legal_entity
  map?)                                ;; TODO child attributes

(s/def ::payout_schedule
  map?)                                ;; TODO child attributes

(s/def ::payout_statement_descriptor
  string?)

(s/def ::payouts_enabled
  boolean?)

(s/def ::product_description
  string?)

(s/def ::statement_descriptor
  ss/statement-descriptor?)

(s/def ::support_email
  string?)                             ;; email address

(s/def ::support_phone
  string?)                             ;; phone number

(s/def ::timezone
  string?)

(s/def ::tos_acceptance
  map?)                                ;; TODO child attributes

(s/def ::type
  string?)

(s/def ::verification
  map?)                                ;; TODO child attributes

(s/def ::account
  (-> (s/keys :req-un [::id ::business_name ::business_url ::charges_enabled ::country
                       ::created ::debit_negative_balances ::decline_charge_on ::default_currency
                       ::details_submitted ::display_name ::email ::external_accounts
                       ::legal_entity ::payout_schedule ::payout_statement_descriptor
                       ::payouts_enabled ::product_description ::statement_descriptor
                       ::support_email ::support_phone ::timezone ::tos_acceptance ::type
                       ::verification])
      (ss/metadata)
      (ss/stripe-object "account")))

(s/def ::accounts
  (ss/sublist ::account))


;; create-params ============================================================


(s/def ::create-params
  (-> (s/keys :opt-un [::country ::business_logo ::business_name
                       ::business_primary_color ::business_url
                       ::debit_negative_balances ::decline_charge_on
                       ::external_account ::legal_entity ::payout_schedule
                       ::payout_statement_descriptor ::product_description
                       ::statement_descriptor ::support_email ::support_phone
                       ::support_url ::tos_acceptance])
      (ss/metadata)))


;; fetch-all-params =========================================================


(s/def ::ending_before
  string?)

(s/def ::limit
  integer?)

(s/def ::starting_after
  string?)

(s/def ::fetch-all-params
  (-> (s/keys :opt-un [::ending_before ::limit ::starting_after])
      (ss/metadata)))


;; update-params ============================================================


(s/def ::business_logo
  string?)                              ;; url for image

(s/def ::business_primary_color
  string?)                              ;; hex value

(s/def ::external_account
  map?)                                 ;; bank account object with child arguments

(s/def ::support_url
  string?)                              ;; url

(s/def ::update-params
  (-> (s/keys :opt-un [::business_logo ::business_name ::business_primary_color ::business_url
                       ::debit_negative_balances ::decline_charge_on ::default_currency
                       ::email ::external_account ::legal_entity ::payout_schedule
                       ::payout_statement_descriptor ::product_description
                       ::statement_descriptor ::support_email ::support_phone ::support_url
                       ::tos_acceptance])
      (ss/metadata)))


;; reject-params ============================================================


(s/def ::reason
  #{"fraud" "terms_of_service" "other"})

(s/def ::reject-params
  (-> (s/keys :req-un [::account ::reason])
      (ss/metadata)))


;; ==========================================================================
;; HTTP API
;; =============================================================================


(defn create!
  "Create a Stripe account for user. Email is required input if account type is standard."
  ([type email]
   (create! type email {} {}))
  ([type email params]
   (create! type email params {}))
  ([type email {:keys [country] :or {country "US"} :as params} opts]
   (let [individual (:individual params)
         params     (->
                      (assoc params :country country :type type :email email)
                      (dissoc :individual))
         account    (h/post-req "accounts" (assoc opts :params params))]
     (h/post-req (str "accounts/" (:id account) "/persons") (assoc opts :params individual))
     account)))

#_(s/fdef create!
        :args (s/alt :binary (s/cat :type ::type
                                    :email ::email)
                     :ternary (s/cat :type ::type
                                     :email ::email
                                     :params ::create-params)
                     :quaternary (s/cat :type ::type
                                        :email ::email
                                        :params ::create-params
                                        :opts h/request-options?))
        :ret (ts/async ::account))


(defn fetch
  "Retrieve account details by identifier, or by API key if no identifier is provided."
  ([account-id]
   (fetch account-id {}))
  ([account-id opts]                                 ;; TODO let account-id be default API key
   (h/get-req (str "accounts/" account-id) opts)))

(s/fdef fetch
        :args (s/cat :account-id ::id
                     :opts (s/? h/request-options?))
        :ret (ts/async ::account))


(defn fetch-external-account
  "Retrieve associated bank or card account (external account) of Connect account."
  ([account-id external-account-id]
   (fetch-external-account account-id external-account-id {}))
  ([account-id external-account-id opts]
   (h/get-req (format "accounts/%s/external_accounts/%s" account-id external-account-id) opts)))

(s/fdef fetch-external-account
        :args (s/cat :account-id ::id
                     :external-account-id ::id
                     :opts (s/? h/request-options?))
        :ret (ts/async ::external-account))


(defn fetch-all
  "Fetch a list of accounts connected to a platform via 'Connect'. Returns empty list if not a platform."
  ([]
   (fetch-all {} {}))
  ([params]
   (fetch-all params {}))
  ([params opts]
   (h/get-req "accounts" (assoc opts :params params))))

(s/fdef fetch-all
        :args (s/alt :nullary (s/cat)
                     :unary (s/cat :params ::fetch-all-params)
                     :binary (s/cat :params ::fetch-all-params
                                    :opts h/request-options?))
        :ret (ts/async ::accounts))


(defn fetch-all-external-accounts
  "Fetch a list of external accounts associated with Connect account."
  ([account-id]
   (fetch-all account-id {} {}))
  ([account-id params]
   (fetch-all account-id params {}))
  ([account-id params opts]
   (h/get-req (format "accounts/%s/external_accounts" account-id) (assoc opts :params params))))

(s/fdef fetch-all-external-accounts
        :args (s/alt :unary (s/cat :account-id ::id)
                     :binary (s/cat :account-id ::id
                                    :params ::fetch-all-params)
                     :ternary (s/cat :account-id ::id
                                     :params ::fetch-all-params
                                     :opts h/request-options?))
        :ret (ts/async ::external_accounts))


(defn update!
  "Update account details for a connected Express or Custom account."
  ([account-id]
   (update! account-id {} {}))
  ([account-id params]
   (update! account-id params {}))
  ([account-id params opts]
   (h/post-req (str "accounts/" account-id) (assoc opts :params params))))

#_(s/fdef update!
        :args (s/alt :unary (s/cat :account-id ::id)
                     :binary (s/cat :account-id ::id
                                    :params ::update-params)
                     :ternary (s/cat :account-id ::id
                                     :params ::update-params
                                     :opts h/request-options?))
        :ret (ts/async ::account))


(defn delete!
  "With 'Connect', delete Custom accounts you manage."
  ([account-id]
   (delete! account-id {}))
  ([account-id opts]
   ;; TODO if no account-id provided defaults to account of API key
   (h/delete-req (str "accounts/" account-id) opts)))

(s/fdef delete!
        :args (s/cat :account-id ::id
                     :opts (s/? h/request-options?))
        :ret (ts/async ss/deleted?))


(defn reject!
  "With 'Connect', flag accounts as suspicious."
  ([account-id reason]
   (reject! account-id reason {}))
  ([account-id reason opts]
   (h/post-req (format "accounts/%s/reject" account-id)
               (assoc-in opts [:params :reason] reason))))

(s/fdef reject!
        :args (s/cat :account-id ::id
                     :reason ::reason
                     :params ::reject-params
                     :opts (s/? h/request-options?))
        :ret (ts/async ::account))


(comment

  (h/use-token! "sk_test_mPUtCMOnGXJwD6RAWMPou8PH")

  (do
    (require '[clojure.spec.test.alpha :as stest])
    (stest/instrument))

  )
