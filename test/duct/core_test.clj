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

(deftest test-load-hierarchy
  (core/load-hierarchy)
  (is (isa? :duct/server :duct/daemon))
  (is (isa? :duct.server/http :duct/server)))

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
         {:foo/a {:x "bar"}
          :foo/b {:bar/a {:x 1}, :bar/b (ig/ref :bar/a)}
          :foo/c (io/resource "duct/config.edn")
          :foo/d (ig/ref :foo/a)
          :foo/e (ig/refset :foo/b)})))

(defmethod ig/init-key ::foo [_ {:keys [x]}]
  #(update % ::x (fnil conj []) x))

(defmethod ig/init-key ::bar [_ {:keys [x]}]
  #(update % ::x (fnil conj []) x))

(deftest test-fold-modules
  (let [m {::foo {:x 1}, ::bar {:x 2, :r (ig/ref ::foo)}}]
    (is (= (core/fold-modules (ig/init m))
           {::x [1 2]}))))

(deftest test-profile-keys
  (let [m {:duct.module/foo    {::a 0}
           :duct.profile/base  {::a 1}
           [:duct/profile ::x] {::a 2}
           [:duct/profile ::y] {::a 3}
           [:duct/profile ::z] {::a 4}}]
    (is (= (set (core/profile-keys m [::x :y]))
           #{:duct.module/foo
             :duct.profile/base
             [:duct/profile ::x]
             [:duct/profile ::y]}))
    (is (= (set (core/profile-keys m :all))
           (set (keys m))))))

(deftest test-parse-keys
  (is (= (seq (core/parse-keys [])) nil))
  (is (= (seq (core/parse-keys [":foo/a" ":bar/b"])) [:foo/a :bar/b])))

(deftest test-profile-keyword
  (core/load-hierarchy)
  (let [m {:duct.profile/base  {::a 1, ::b (ig/ref ::a)}
           [:duct/profile ::x] {::a 2, ::c (ig/refset ::b)}}
        p (ig/prep m)]
    (is (= p
           {:duct.profile/base  {::a 1, ::b (core/->InertRef ::a)}
            [:duct/profile ::x] {::a 2, ::c (core/->InertRefSet ::b)
                                 ::core/requires (ig/ref :duct.profile/base)}}))
    (is (= (core/fold-modules (ig/init p))
           {::a 2, ::b (ig/ref ::a), ::c (ig/refset ::b)}))))

(deftest test-build-config
  (core/load-hierarchy)
  (let [m {:duct.profile/base  {::a 1, ::b (ig/ref ::a)}
           [:duct/profile ::x] {::a 2, ::c (ig/refset ::b)}
           [:duct/profile ::y] {::d 3}}]
    (is (= (core/build-config m)
           {::a 2, ::b (ig/ref ::a), ::c (ig/refset ::b), ::d 3}))
    (is (= (core/build-config m [::x])
           {::a 2, ::b (ig/ref ::a), ::c (ig/refset ::b)}))
    (is (= (core/build-config m [:y])
           {::a 1, ::b (ig/ref ::a), ::d 3}))))

(defmethod ig/prep-key ::prep [_ v] (inc v))

(deftest test-prep-config
  (let [m {:duct.profile/base  {::prep 1, ::a (ig/ref ::prep)}
           [:duct/profile ::x] {::prep 2, ::b (ig/refset ::a)}
           [:duct/profile ::y] {::c 3}}]
    (is (= (core/prep-config m)
           {::prep 3, ::a (ig/ref ::prep), ::b (ig/refset ::a), ::c 3}))
    (is (= (core/prep-config m [::x])
           {::prep 3, ::a (ig/ref ::prep), ::b (ig/refset ::a)}))))

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

(deftest test-profile-dev-keyword
  (core/load-hierarchy)
  (let [m {:duct.profile/base {::a 1, ::b (ig/ref ::a)}
           :duct.profile/dev  {::a 2, ::c (ig/refset ::b)}}
        p (ig/prep m)]
    (is (= p
           {:duct.profile/base {::a 1, ::b (core/->InertRef ::a)}
            :duct.profile/dev  {::a 2, ::c (core/->InertRefSet ::b)
                                ::core/requires (ig/ref :duct.profile/base)
                                ::core/environment :development}}))
    (is (= (core/fold-modules (ig/init p))
           {::a 2, ::b (ig/ref ::a), ::c (ig/refset ::b)
            ::core/environment :development}))))

(deftest test-profile-prod-keyword
  (core/load-hierarchy)
  (let [m {:duct.profile/base {::a 1, ::b (ig/ref ::a)}
           :duct.profile/prod {::a 2, ::c (ig/refset ::b)}}
        p (ig/prep m)]
    (is (= p
           {:duct.profile/base {::a 1, ::b (core/->InertRef ::a)}
            :duct.profile/prod {::a 2, ::c (core/->InertRefSet ::b)
                                ::core/requires (ig/ref :duct.profile/base)
                                ::core/environment :production}}))
    (is (= (core/fold-modules (ig/init p))
           {::a 2, ::b (ig/ref ::a), ::c (ig/refset ::b)
            ::core/environment :production}))))
