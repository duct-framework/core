(ns duct.core
  "Core functions required by a Duct application."
  (:refer-clojure :exclude [compile])
  (:require [clojure.core :as core]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as str]
            [duct.core.env :as env]
            [duct.core.merge :as merge]
            [integrant.core :as ig]
            [medley.core :as m]
            [clojure.walk :as walk]))

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

(defn- config-resource [path]
  (or (io/resource path)
      (io/resource (str path ".edn"))
      (io/resource (str path ".clj"))))

(defn- make-include [readers]
  #(some->> % config-resource slurp (ig/read-string {:readers readers})))

(defn- merge-default-readers [readers]
  (merge
   {'duct/env      env/env
    'duct/include  (make-include readers)
    'duct/resource io/resource}
   readers))

(defn read-config
  "Read an edn configuration from a slurpable source. An optional map of data
  readers may be supplied. By default the following five readers are supported:

  #duct/env
  : an environment variable, see [[duct.core.env/env]]
  
  #duct/include
  : substitute for a configuration on the classpath

  #duct/resource
  : a resource path string, see clojure.java.io/resource

  #ig/ref
  : an Integrant reference to another key

  #ig/refset
  : an Integrant reference to a set of keys"
  ([source]
   (read-config source {}))
  ([source readers]
   (some->> source slurp (ig/read-string {:readers (merge-default-readers readers)}))))

(defn fold-modules
  "Fold a system map of modules into an Integrant configuration. A module is a
  pure function that transforms a configuration map. The modules are traversed
  in dependency order and applied to iteratively to a blank map in order to
  build the final configuration."
  [system]
  (ig/fold system (fn [m _ f] (f m)) {}))

(defn- matches-name? [key profile-key]
  (letfn [(matches? [k] (= (name k) (name profile-key)))]
    (if (vector? key)
      (some matches? key)
      (matches? key))))

(defn- matches-profile? [key profile-key]
  (if (namespace profile-key)
    (ig/derived-from? key profile-key)
    (matches-name? key profile-key)))

(defn- keep-key? [profiles key]
  (or (not (ig/derived-from? key :duct/profile))
      (ig/derived-from? key :duct.profile/base)
      (some (partial matches-profile? key) profiles)))

(defn profile-keys
  "Return a collection of keys for a configuration that excludes any profile
  not present in the supplied colleciton of profiles. Profiles may be specified
  as namespaced keywords, or as un-namespaced keywords, in which case only the
  name will matched (e.g. `:dev` will match `:duct.profile/dev`)."
  [config profiles]
  (filter (partial keep-key? profiles) (keys config)))

(defn build-config
  "Build an Integrant configuration from a configuration of modules. A
  collection of profile keys may optionally be supplied that govern which
  profiles to use (see [[profile-keys]])."
  ([config]
   (-> config ig/prep ig/init fold-modules))
  ([config profiles]
   (let [keys (profile-keys config profiles)]
     (-> config ig/prep (ig/init keys) fold-modules))))

(defn parse-keys
  "Parse config keys from a sequence of command line arguments."
  [args]
  (seq (filter keyword? (map edn/read-string args))))

(defn- has-daemon? [system]
  (seq (ig/find-derived system :duct/daemon)))

(defn await-daemons
  "If the supplied system has keys deriving from `:duct/daemon`, block the
  current thread indefinitely and add a shutdown hook to halt the system.

  This function is designed to be called from `-main` when standalone operation
  is required."
  [system]
  (when (has-daemon? system)
    (add-shutdown-hook ::exec #(ig/halt! system))
    (.. Thread currentThread join)))

(defrecord InertRef    [key])
(defrecord InertRefSet [key])

(defn- deactivate-ref [x]
  (cond
    (ig/ref? x)    (->InertRef (:key x))
    (ig/refset? x) (->InertRefSet (:key x))
    :else x))

(defn- activate-ref [x]
  (cond
    (instance? InertRef x)    (ig/ref (:key x))
    (instance? InertRefSet x) (ig/refset (:key x))
    :else x))

(defmethod ig/prep-key :duct/module [_ profile]
  (assoc profile ::requires (ig/refset :duct/profile)))

(defmethod ig/init-key ::environment [_ env] env)

(defmethod ig/init-key ::project-ns [_ ns] ns)

(defmethod ig/init-key ::handler [_ {:keys [middleware router]}]
  ((apply comp (reverse middleware)) router))

(defmethod ig/prep-key :duct/profile [_ profile]
  (-> (walk/postwalk deactivate-ref profile)
      (assoc ::requires (ig/ref :duct.profile/base))))

(defmethod ig/init-key :duct/profile [_ profile]
  (let [profile (walk/postwalk activate-ref (dissoc profile ::requires))]
    #(merge-configs % profile)))

(defmethod ig/prep-key :duct.profile/base [_ profile]
  (walk/postwalk deactivate-ref profile))

(defmethod ig/prep-key :duct.profile/dev [_ profile]
  (-> (ig/prep-key :duct/profile profile)
      (assoc ::environment :development)))

(defmethod ig/prep-key :duct.profile/prod [_ profile]
  (-> (ig/prep-key :duct/profile profile)
      (assoc ::environment :production)))
