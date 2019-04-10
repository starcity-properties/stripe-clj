(defproject starcity/stripe-clj "0.4.2-SNAPSHOT"
  :description "Stripe bindings for Clojure."
  :url "https://github.com/starcity-properties/stripe-clj"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/core.async "0.4.474"]
                 [environ/environ.core "0.3.1"]
                 [http-kit "2.2.0"]
                 [cheshire "5.8.0"]
                 [ring/ring-codec "1.1.0"]
                 [starcity/toolbelt-spec "0.1.5"]
                 [starcity/toolbelt-async "0.4.0"]
                 [starcity/toolbelt-core "0.3.0"]]

  :plugins [[lein-codox "0.10.3"]]

  :source-paths ["src/clj"]

  :repl-options {:init-ns user}

  :deploy-repositories [["releases" {:url   "https://clojars.org/repo"
                                     :creds :gpg}]]

  :profiles {:test {:dependencies [[org.clojure/test.check "0.10.0-alpha2"]
                                   [se.haleby/stub-http "0.2.4"]]}})
