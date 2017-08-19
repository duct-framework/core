(ns duct.core
  "Core functions required by a Duct application."
  (:refer-clojure :exclude [compile])
  (:require [com.stuartsierra.dependency :as dep]
            [clojure.core :as core]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as str]
            [duct.core.env :as env]
            [duct.core.merge :as merge]
            [integrant.core :as ig]
            [medley.core :as m]))

(def target-path
  "A path to place generated files in. Typically used by compilers. Can be set
  via the `duct.target.path` system property."
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

(defn- merge-configs* [a b]
  (merge/meta-merge (expand-ancestor-keys a b)
                    (expand-ancestor-keys b a)))

(defn merge-configs
  "Intelligently merge multiple configurations. Uses meta-merge and will merge
  configurations in order from left to right. Generic top-level keys are merged
  into more specific descendants, if the descendants exist."
  [& configs]
  (merge/unwrap-all (reduce merge-configs* {} configs)))

(def ^:private default-readers
  {'duct/resource io/resource
   'duct/env      env/env})

(defn read-config
  "Read an edn configuration from a slurpable source. An optional map of data
  readers may be supplied. By default the following three readers are
  supported:

  #ig/ref
  : an Integrant reference to another key
  
  #duct/resource
  : a resource path string, see clojure.java.io/resource
  
  #duct/env
  : an environment variable, see [[duct.core.env/env]]"
  ([source]
   (read-config source {}))
  ([source readers]
   (some->> source slurp (ig/read-string {:readers (merge default-readers readers)}))))

(declare apply-includes)

(defn- config-resource [path]
  (or (io/resource path)
      (io/resource (str path ".edn"))
      (io/resource (str path ".clj"))))

(defn- load-config-resource [path reader]
  (-> (config-resource path) (reader) (apply-includes reader)))

(defn- load-includes [config reader]
  (mapv #(load-config-resource % reader) (::include config)))

(defn- apply-includes [config reader]
  (apply merge-configs (conj (load-includes config reader) config)))

(defn- can-apply-module? [config [_ {requires :req}]]
  (every? #(seq (ig/find-derived config %)) requires))

(defn- find-applicable-module [config modules]
  (m/find-first (partial can-apply-module? config) modules))

(defn- missing-requirements-exception [config modules applied]
  (let [missing (m/map-vals #(set/difference (set (:req %)) (set (keys config))) modules)]
    (ex-info (str "Missing module requirements: "
                  (str/join ", " (for [[k v] missing] (str k " requires " (sort v)))))
             {:reason    ::missing-requirements
              :missing   missing
              :config    config
              :applied   applied
              :remaining (keys modules)})))

(defn- init-modules [config]
  (-> config
      (ig/init [:duct/module])
      (ig/find-derived :duct/module)))

(defn- apply-modules [config]
  (loop [modules (into (sorted-map) (init-modules config))
         applied []
         config  config]
    (if (seq modules)
      (if-let [[k m] (find-applicable-module config modules)]
        (recur (dissoc modules k) (conj applied k) ((:fn m) config))
        (throw (missing-requirements-exception config modules applied)))
      config)))

(defn prep
  "Prep a configuration, ready to be initiated. Key namespaces are loaded,
  resources included, and modules applied.

  An optional map of data readers may be supplied to be used when reading
  configurations imported via `:duct.core/include`."
  ([config]
   (prep config {}))
  ([config readers]
   (-> config
       (apply-includes (memoize #(read-config % readers)))
       (doto ig/load-namespaces)
       (apply-modules)
       (doto ig/load-namespaces))))

(defn parse-keys
  "Parse config keys from a sequence of command line arguments."
  [args]
  (filter keyword? (map edn/read-string args)))

(defn- has-daemon? [system]
  (seq (ig/find-derived system :duct/daemon)))

(defn exec
  "Initiate a prepped configuration with an optional collection of keys.

  If the collection of keys is empty, all keys deriving from `:duct/daemon` are
  used. If any initiated key derives from `:duct/daemon`, this function will
  block indefinitely and add a shutdown hook to halt the system.

  This function is designed to be called from `-main` when standalone operation
  is required."
  ([config]
   (exec config nil))
  ([config keys]
   (let [keys   (or (seq keys) [:duct/daemon])
         system (ig/init config keys)]
     (when (has-daemon? system)
       (add-shutdown-hook ::exec #(ig/halt! system))
       (.. Thread currentThread join)))))

(defn- hierarchy-urls []
  (let [cl (.. Thread currentThread getContextClassLoader)]
    (enumeration-seq (.getResources cl "duct_hierarchy.edn"))))

(defn load-hierarchy
  "Search the base classpath for files named `duct_hierarchy.edn`, and use them
  to extend the global `derive` hierarchy. This allows a hierarchy to be
  constructed without needing to load every namespace.

  The `duct_hierarchy.edn` file should be an edn map that maps child keywords
  to vectors of parents. For example:

      {:example/child [:example/father :example/mother]}

  This is equivalent to writing:

      (derive :example/child :example/father)
      (derive :example/child :example/mother)

  This function should be called once when the application is started."
  []
  (doseq [url (hierarchy-urls)]
    (let [hierarchy (edn/read-string (slurp url))]
      (doseq [[tag parents] hierarchy, parent parents]
        (derive tag parent)))))

(defmethod ig/init-key ::environment [_ env] env)

(defmethod ig/init-key ::project-ns [_ ns] ns)

(defmethod ig/init-key ::include [_ paths] paths)

(defmethod ig/init-key ::handler [_ {:keys [middleware router]}]
  ((apply comp (reverse middleware)) router))
