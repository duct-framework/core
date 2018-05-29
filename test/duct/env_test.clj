(ns duct.env-test
  (:require [clojure.test :refer :all]
            [duct.core.env :as env])
  (:import (clojure.lang ExceptionInfo)))

(deftest coerce
  (are [a b c] (= (env/coerce a b) c)
    "a"    'Str  "a"
    12     'Str  "12"
    "123"  'Int  123
    "true" 'Bool true
    "TruE" 'Bool true
    "t"    'Bool true
    ""     'Bool false
    "no"   'Bool false
    nil    'Bool false)
  (is (thrown? IllegalArgumentException
               (env/coerce "abc" 'Int)))
  (is (thrown? ExceptionInfo
               (env/coerce "tru" 'Bool))))
