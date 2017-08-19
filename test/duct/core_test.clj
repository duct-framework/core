(ns duct.core-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer :all]
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
    {::a 1}                {::a 2}                       {::a 2}
    {::a {:x 1}}           {::a {:y 2}}                  {::a {:x 1 :y 2}}
    {::a {:x 1}}           {::a ^:displace {:x 2}}       {::a {:x 1}}
    {}                     {::a ^:displace {:y 2}}       {::a {:y 2}}
    {::aa 1}               {::a 2}                       {::aa 2}
    {::aa 1 ::ab 2}        {::a 3}                       {::aa 3 ::ab 3}
    {::aa {:x 1}}          {::a {:y 2}}                  {::aa {:x 1 :y 2}}
    {::a 1}                {::aa 2}                      {::aa 2}
    {::a {:x 1}}           {::aa {:y 2}}                 {::aa {:x 1 :y 2}}
    {::a {:x 1}}           {::aa {:y 2} ::ab {:z 3}}     {::aa {:x 1 :y 2} ::ab {:x 1 :z 3}}
    {::a 1}                {::a (merge/displace 2)}      {::a 1}
    {::a {:x 1}}           {::a {:x (merge/displace 2)}} {::a {:x 1}}
    {::a [:x :y]}          {::a [:y :z]}                 {::a [:x :y :y :z]}
    {::a [:x :y]}          {::a ^:distinct [:y :z]}      {::a [:x :y :z]}
    {::a {:x 1}}           {::a ^:demote {:x 2, :y 3}}   {::a {:x 1, :y 3}}
    {::a ^:promote {:x 1}} {::a {:x 2, :y 3}}            {::a {:x 1, :y 3}}))

(deftest test-read-config
  (is (= (core/read-config (io/resource "duct/readers.edn") {'custom/bar (fn [x] {:x x})})
         {:foo (io/resource "duct/config.edn")
          :bar {:x "bar"}})))

(derive ::xx ::x)

(derive ::mod1 :duct/module)
(derive ::mod2 :duct/module)
(derive ::mod3 :duct/module)

(defmethod ig/init-key ::x [_ x] x)

(defmethod ig/init-key ::mod1 [_ _]
  {:fn (fn [cfg] (assoc cfg ::xx 1))})

(defmethod ig/init-key ::mod2 [_ _]
  {:req #{::xx}, :fn (fn [cfg] (assoc cfg ::y (inc (::xx cfg))))})

(defmethod ig/init-key ::mod3 [_ _]
  {:req #{::x ::y}, :fn (fn [cfg] (assoc cfg ::z (+ (::xx cfg) (::y cfg))))})

(deftest test-prep
  (testing "includes"
    (let [config {::core/include ["duct/config"]
                  ::b {:x 2}
                  ::c {:x 3}}]
      (is (= (core/prep config)
             {::core/include ["duct/included" "duct/config"]
              ::a {:x 1}
              ::b {:x 2, :y 2}
              ::c {:x 3}}))))

  (testing "include with custom reader"
    (let [config {::core/include ["duct/reader"]}]
      (is (= (core/prep config {'duct/inc inc})
             {::core/include ["duct/reader"]
              ::a {:x 2}}))))

  (testing "valid modules"
    (let [config {::mod1 {}, ::mod2 {}, ::mod3 {}}]
      (is (= (core/prep config)
             (merge config {::xx 1, ::y 2, ::z 3})))))

  (testing "missing requirements"
    (let [config {::mod2 {}, ::mod3 {}}]
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo
           (re-pattern
            (str "Missing module requirements: "
                 ::mod2 " requires \\(" ::xx "\\), "
                 ::mod3 " requires \\(" ::x " " ::y "\\)"))
           (core/prep config)))))

  (testing "valid modules with dependencies"
    (let [config {::mod1 {:x (ig/ref ::x)},
                  ::mod2 {}
                  ::mod3 {}
                  ::xx 1}]
      (is (= (core/prep config)
             (merge config {::xx 1, ::y 2, ::z 3}))))))

(deftest test-load-hierarchy
  (core/load-hierarchy)
  (is (isa? :duct/server :duct/daemon))
  (is (isa? :duct.server/http :duct/server)))

(deftest test-environment-keyword
  (let [m {::core/environment :development}]
    (is (= m (ig/init m)))))

(deftest test-project-ns-keyword
  (let [m {::core/project-ns 'foo}]
    (is (= m (ig/init m)))))

(deftest test-handler-keyword
  (let [m {::core/handler
           {:router     (fn [_] {:status 200, :headers {}, :body "foo"})
            :middleware [(fn [f] #(assoc-in (f %) [:headers "X-Foo"] "bar"))
                         (fn [f] #(assoc-in (f %) [:headers "X-Foo"] "baz"))]}}
        f (::core/handler (ig/init m))]
    (is (= (f {:request-method :get, :uri "/"})
           {:status 200, :headers {"X-Foo" "baz"}, :body "foo"}))))
