(ns duct.repl-test
  (:require [clojure.test :refer :all]
            [duct.core :as core]
            [duct.core.repl :as repl]
            [fipp.edn :as fipp]
            [integrant.core :as ig]))

(deftest test-pprint
  (is (= (with-out-str (fipp/pprint
                        {:a (ig/ref :duct.router/cascading)
                         :b (ig/refset :duct/module)
                         :c (core/resource "duct/core.clj")}))
         (str "{:a #ig/ref :duct.router/cascading,\n"
              " :b #ig/refset :duct/module,\n"
              " :c #duct/resource \"duct/core.clj\"}\n"))))
