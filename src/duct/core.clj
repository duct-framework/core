(ns duct.core
  "Core functions required by a Duct application."
  (:refer-clojure :exclude [compile])
  (:require [clojure.java.io :as io]
            [duct.core.env :as env]
            [duct.core.merge :as merge]
            [integrant.core :as ig]))

(def target-path
  "A path to place generated files in. Typically used by compilers. Can be set
  via the duct.target.path system property."
  (or (System/getProperty "duct.target.path")
      (.getAbsolutePath (io/file "target"))))

(def ^:private hooks (atom {}))

(defn- run-hooks []
  (doseq [f (vals @hooks)] (f)))

(defonce ^:private init-shutdown-hook
  (delay (.addShutdownHook (Runtime/getRuntime) (Thread. #'run-hooks))))

(defn add-shutdown-hook
  "Set a function to be executed when the current process shuts down. The key
  argument should be unique, and is used in [[remove-shutdown-hook]]."
  [key func]
  (force init-shutdown-hook)
  (swap! hooks assoc key func))

(defn remove-shutdown-hook
  "Remove a shutdown hook identified by the specified key."
  [key]
  (swap! hooks dissoc key))

(defn- expand-ancestor-keys [config base]
  (reduce-kv
   (fn [m k v]
     (if-let [ks (seq (keys (ig/find-derived base k)))]
       (reduce #(assoc %1 %2 v) m ks)
       (assoc m k v)))
   {}
   config))

(defn merge-configs
  "Intelligently merge multiple configurations. Uses meta-merge and will merge
  configurations in order from left to right. Generic top-level keys are merged
  into more specific descendants, if the descendants exist."
  ([a b]
   (merge/meta-merge (expand-ancestor-keys a b)
                     (expand-ancestor-keys b a)))
  ([a b & more]
   (reduce merge-configs (merge-configs a b) more)))

(def ^:private readers
  {'resource io/resource
   'env      env/env})

(defn read-config
  "Read an edn configuration from one or more slurpable sources. Multiple
  sources are merged together with merge-configs. Three additional data readers
  are supported:

  #ref
  : an Integrant reference to another key
  
  #resource
  : a resource path string, see clojure.java.io/resource
  
  #env
  : an environment variable, see [[duct.core.env/env]]"
  ([source]
   (some->> source slurp (ig/read-string {:readers readers})))
  ([source & sources]
   (apply merge-configs (read-config source) (map read-config sources))))

(defn- apply-modules [config]
  (if (contains? config ::modules)
    (let [modules (::modules (ig/init config [::modules]))]
      (modules config))
    config))

(defn- derived-keys [config k]
  (map key (ig/find-derived config k)))

(defn- dissoc-derived [config key]
  (apply dissoc config (derived-keys config key)))

(defn prep
  "Prep a configuration, ready to be initiated. Key namespaces are loaded,
  and modules are applied."
  [config]
  (-> config
      (doto ig/load-namespaces)
      (apply-modules)
      (doto ig/load-namespaces)))

(defn compile
  "Prep then initiate all keys that derive from `:duct/compiler`."
  [config]
  (ig/init (prep config) [:duct/compiler]))

(defn exec
  "Prep then initiate a configuration, excluding keys that derive from
  `:duct/compiler`, and then block indefinitely. This function should be called
  from `-main` when a standalone application is required."
  [config]
  (let [system (-> config prep (dissoc-derived :duct/compiler) ig/init)]
    (add-shutdown-hook ::exec #(ig/halt! system))
    (.. Thread currentThread join)))

(defmethod ig/init-key ::modules [_ modules]
  (apply comp (reverse modules)))

(defmethod ig/init-key ::environment [_ env] env)

(defmethod ig/init-key ::project-ns [_ ns] ns)
