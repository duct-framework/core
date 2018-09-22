(ns duct.resource-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [duct.core.resource :as resource]
            [fipp.edn :as fipp]))

(deftest test-io
  (is (= (slurp (resource/make-resource "duct/core.clj"))
         (slurp (io/resource "duct/core.clj"))))
  (is (= (io/as-url (resource/make-resource "duct_hierarchy.edn"))
         (io/resource "duct_hierarchy.edn"))))
