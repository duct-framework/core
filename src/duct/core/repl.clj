(ns duct.core.repl
  (:require [duct.core.resource :as resource]
            [fipp.ednize :as fipp]
            [integrant.core :as ig]
            [integrant.repl :refer [reset]]
            [hawk.core :as hawk]))

(extend-type integrant.core.Ref
  fipp/IOverride
  fipp/IEdn
  (-edn [r] (tagged-literal 'ig/ref (:key r))))

(extend-type integrant.core.RefSet
  fipp/IOverride
  fipp/IEdn
  (-edn [r] (tagged-literal 'ig/refset (:key r))))

(extend-type duct.core.resource.Resource
  fipp/IOverride
  fipp/IEdn
  (-edn [r] (tagged-literal 'duct/resource (:path r))))

(defn- clojure-file? [_ {:keys [file]}]
  (re-matches #"[^.].*(\.clj|\.edn)$" (.getName file)))

(defn- auto-reset-handler [ctx event]
  (binding [*ns* *ns*]
    (integrant.repl/reset)
    ctx))

(defn auto-reset
  "Automatically reset the system when a Clojure or edn file is changed in
  `src` or `resources`."
  []
  (hawk/watch! [{:paths ["src/" "resources/" "dev/src/" "dev/resources/"]
                 :filter clojure-file?
                 :handler auto-reset-handler}]))
