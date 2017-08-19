(ns duct.repl-test
  (:require [clojure.test :refer :all]
            [duct.core.repl :as repl]
            [fipp.edn :as fipp]
            [integrant.core :as ig]))

(deftest test-pprint
  (is (= (with-out-str (fipp/pprint (ig/ref :duct.router/cascading)))
         "#ig/ref :duct.router/cascading\n")))
