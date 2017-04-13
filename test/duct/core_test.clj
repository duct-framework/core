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

(derive ::aa ::a)
(derive ::ab ::a)
(derive ::ab ::b)

(deftest test-merge-configs
  (are [a b c] (= (core/merge-configs a b) c)
    {::a 1}         {::a 2}                 {::a 2}
    {::a {:x 1}}    {::a {:y 2}}            {::a {:x 1 :y 2}}
    {::a {:x 1}}    {::a ^:displace {:x 2}} {::a {:x 1}}
    {}              {::a ^:displace {:y 2}} {::a {:y 2}}
    {::aa 1}        {::a 2}                 {::aa 2}
    {::aa 1 ::ab 2} {::a 3}                 {::aa 3 ::ab 3}
    {::aa {:x 1}}   {::a {:y 2}}            {::aa {:x 1 :y 2}}))
