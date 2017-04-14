(ns duct.core-test
  (:require [clojure.test :refer :all]
            [duct.core :as core]
            [duct.core.merge :as merge]
            [integrant.core :as ig]))

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
    {::a 1}         {::a 2}                       {::a 2}
    {::a {:x 1}}    {::a {:y 2}}                  {::a {:x 1 :y 2}}
    {::a {:x 1}}    {::a ^:displace {:x 2}}       {::a {:x 1}}
    {}              {::a ^:displace {:y 2}}       {::a {:y 2}}
    {::aa 1}        {::a 2}                       {::aa 2}
    {::aa 1 ::ab 2} {::a 3}                       {::aa 3 ::ab 3}
    {::aa {:x 1}}   {::a {:y 2}}                  {::aa {:x 1 :y 2}}
    {::a 1}         {::aa 2}                      {::aa 2}
    {::a {:x 1}}    {::aa {:y 2}}                 {::aa {:x 1 :y 2}}
    {::a {:x 1}}    {::aa {:y 2} ::ab {:z 3}}     {::aa {:x 1 :y 2} ::ab {:x 1 :z 3}}
    {::a 1}         {::a (merge/displace 2)}      {::a 1}
    {::a {:x 1}}    {::a {:x (merge/displace 2)}} {::a {:x 1}}))

(deftest test-modules-keyword
  (let [m (ig/init {::core/modules [(partial * 2) (partial + 3)]})
        f (::core/modules m)]
    (is (= (f 7) 17))))

(deftest test-environment-keyword
  (let [m {::core/environment :development}]
    (is (= m (ig/init m)))))

(deftest test-project-ns-keyword
  (let [m {::core/project-ns 'foo}]
    (is (= m (ig/init m)))))
