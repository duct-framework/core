(ns duct.repl-test
  (:require [clojure.test :refer :all]
            [duct.core.repl :as repl]
            [fipp.edn :as fipp]
            [integrant.core :as ig]))

(deftest test-pprint
  (is (= (with-out-str (fipp/pprint
                        {:a (ig/ref :duct.router/cascading)
                         :b (ig/refset :duct/module)}))
         (str "{:a #ig/ref :duct.router/cascading,"
              " :b #ig/refset :duct/module}\n"))))
