(ns duct.core.env
  "Functions for pulling values from environment variables."
  (:require [clojure.string :as str]))

(defmulti coerce
  "Coerce a value to the type referenced by a symbol. By default supports
  `Int` and `Str`."
  (fn [x type] type))

(defmethod coerce 'Int [x _]
  (Long/parseLong x))

(defmethod coerce 'Double [x _]
  (Double/parseDouble x))

(defmethod coerce 'Str [x _]
  (str x))

(defmethod coerce 'Keyword [x _]
  (keyword x))

(defmethod coerce 'Bool [x _]
  (case (some-> x str/lower-case)
    ("true" "t" "yes" "y") true
    ("false" "f" "no" "n" "" nil) false
    (throw (ex-info (str "Could not coerce '" (pr-str x) "' into a boolean."
                         "Must be one of: \"true\", \"t\", \"false\", \"f\","
                         "\"yes\", \"y\", \"no\", \"n\", \"\" or nil.")
                    {:value x, :coercion 'Bool}))))

(def ^:dynamic *env*
  (into {} (System/getenv)))

(defn env
  "Resolve an environment variable by name. Optionally accepts a type for
  coercion, and a keyword option, `:or`, that provides a default value if the
  environment variable is missing.

  The arguments may optionally be wrapped in a vector. This is to support
  their use in [[duct.core/read-config]]. For example:

      {:port #duct/env [\"PORT\" Int :or 3000]}"
  ([name]
   (if (vector? name)
     (apply env name)
     (*env* name)))
  ([name type & options]
   (if (keyword? type)
     (apply env name 'Str type options)
     (let [{default :or} options
           value (*env* name)]
       (if (nil? value)
         default
         (coerce value type))))))
