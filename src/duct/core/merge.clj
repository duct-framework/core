(ns duct.core.merge
  (:refer-clojure :exclude [replace distinct?])
  (:require [clojure.set :as set]
            [clojure.walk :as walk]
            [duct.core.merge :as merge]
            [integrant.core :as ig]))

(defrecord WrapMeta [val])

(defn wrap
  "If a value doesn't support metadata, wrap it in a record that does."
  [val]
  (if (instance? clojure.lang.IObj val) val (->WrapMeta val)))

(defn unwrap
  "Unwrap a value, if it is wrapped. Reverses the [[wrap]] function."
  [obj]
  (if (instance? WrapMeta obj) (:val obj) obj))

(defn unwrap-all
  "Unwrap all values nested in a data structure."
  [x]
  (walk/postwalk unwrap x))

(defn displace
  "Add displace metadata to a value."
  [val]
  (vary-meta (wrap val) assoc :displace true))

(defn replace
  "Add replace metadata to a value."
  [val]
  (vary-meta (wrap val) assoc :replace true))

(defn- meta* [obj]
  (if (instance? clojure.lang.IObj obj) (meta obj)))

(defn- displace? [obj]
  (-> obj meta* :displace))

(defn- replace? [obj]
  (-> obj meta* :replace))

(defn- different-priority? [left right]
  (some (some-fn nil? displace? replace?) [left right]))

(defn- pick-prioritized [left right]
  (cond
    (nil? left) right
    (nil? right) left

    (and (displace? left)   ;; Pick the rightmost
         (displace? right)) ;; if both are marked as displaceable
    (with-meta (wrap right)
      (merge (meta* left) (meta* right)))

    (and (replace? left)    ;; Pick the rightmost
         (replace? right))  ;; if both are marked as replaceable
    (with-meta (wrap right)
      (merge (meta* left) (meta* right)))

    (or (displace? left)
        (replace? right))
    (with-meta (wrap right)
      (merge (-> left meta* (dissoc :displace))
             (-> right meta* (dissoc :replace))))

    (or (replace? left)
        (displace? right))
    (with-meta (wrap left)
      (merge (-> right meta* (dissoc :displace))
             (-> left meta* (dissoc :replace))))))

(defn- prepend? [obj]
  (-> obj meta :prepend))

(defn- distinct? [obj]
  (-> obj meta :distinct))

(defn- meta-concat [left right]
  (let [combined (concat left right)]
    (into (empty left)
          (if (or (distinct? left) (distinct? right))
            (distinct combined)
            combined))))

(defn meta-merge
  "Recursively merge values based on the information in their metadata."
  [left right]
  (cond
    (different-priority? left right)
    (pick-prioritized left right)

    (and (map? left) (map? right))
    (merge-with meta-merge left right)

    (and (set? left) (set? right))
    (set/union right left)

    (and (coll? left) (coll? right))
    (if (or (prepend? left) (prepend? right))
      (-> (meta-concat right left)
          (with-meta (merge (meta left) (select-keys (meta right) [:displace]))))
      (meta-concat left right))

    :else right))
