(defproject duct/core "0.9.0-SNAPSHOT"
  :description "The core library for the Duct framework"
  :url "https://github.com/duct-framework/core"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [integrant "0.3.3"]
                 [meta-merge "1.0.0"]]
  :plugins [[lein-codox "0.10.3"]]
  :codox {:output-path "codox"
          :source-uri
          "https://github.com/duct-framework/core/blob/{version}/{filepath}#L{line}"})
