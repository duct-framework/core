(ns duct.env-test
  (:require [clojure.test :refer :all]
            [duct.core.env :as env])
  (:import (clojure.lang ExceptionInfo)))

(deftest coerce
  (are [a b c] (= (env/coerce a b) c)
    "a"    'Str    "a"
    12     'Str    "12"
    "123"  'Int    123
    "1.2"  'Double 1.2
    "true" 'Bool   true
    "TruE" 'Bool   true
    "t"    'Bool   true
    ""     'Bool   false
    "no"   'Bool   false
    nil    'Bool   false)
  (is (thrown? IllegalArgumentException
               (env/coerce "abc" 'Int)))
  (is (thrown? NumberFormatException
               (env/coerce "abc" 'Double)))
  (is (thrown? NumberFormatException
               (env/coerce "" 'Double)))
  (is (thrown? ExceptionInfo
               (env/coerce "tru" 'Bool))))

(deftest env
  (binding [env/*env* {"INTEGER"       "2000"
                       "DOUBLE"        "1.2"
                       "STRING"        "a string"
                       "BOOLEAN_TRUE"  "true"
                       "BOOLEAN_FALSE" "false"}]
    (are [v expected] (= expected (env/env v))
      '["UNDEFINED" Int]                nil
      '["UNDEFINED" Int :or 3001]       3001
      '["INTEGER" Int]                  2000
      '["INTEGER" Int :or 3002]         2000
      '["DOUBLE" Double]                1.2
      '["DOUBLE" Double :or 1.3]        1.2
      '["UNDEFINED" Bool]               nil
      '["UNDEFINED" Bool :or true]      true
      '["UNDEFINED" Bool :or false]     false
      '["BOOLEAN_TRUE" Bool]            true
      '["BOOLEAN_TRUE" Bool :or true]   true
      '["BOOLEAN_TRUE" Bool :or false]  true
      '["BOOLEAN_FALSE" Bool]           false
      '["BOOLEAN_FALSE" Bool :or true]  false
      '["BOOLEAN_FALSE" Bool :or false] false)))
