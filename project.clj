(defproject duct/core "0.5.1"
  :description "The core library for the Duct framework"
  :url "https://github.com/duct-framework/core"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha17"]
                 [integrant "0.5.0"]
                 [medley "1.0.0"]
                 [fipp "0.6.9"]]
  :plugins [[lein-codox "0.10.3"]]
  :codox {:output-path "codox"
          :project  {:name "Duct core"}
          :html     {:namespace-list :flat}
          :metadata {:doc/format :markdown}
          :source-uri
          "https://github.com/duct-framework/core/blob/{version}/{filepath}#L{line}"})
