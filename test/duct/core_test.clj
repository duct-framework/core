(ns duct.core-test
  (:require [clojure.test :refer :all]
            [duct.core :as core]))

(deftest test-add-shutdown-hook
  (let [f #(identity true)
        hooks (core/add-shutdown-hook ::foo f)]
    (is (= f (::foo hooks)))))

(deftest test-remove-shutdown-hook
  (core/add-shutdown-hook ::foo #(identity true))
  (let [hooks (core/remove-shutdown-hook ::foo)]
    (is (nil? (::foo hooks)))))
