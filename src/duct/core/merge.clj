(ns duct.core.merge
  (:require [clojure.set :as set]))

(defn- meta* [obj]
  (if (instance? clojure.lang.IObj obj)
    (meta obj)
    nil))

(defn- with-meta* [obj m]
  (if (instance? clojure.lang.IObj obj)
    (with-meta obj m)
    obj))

(defn- displace? [obj]
  (-> obj meta* :displace))

(defn- replace? [obj]
  (-> obj meta* :replace))

(defn- different-priority? [left right]
  (boolean (some (some-fn nil? displace? replace?) [left right])))

(defn- pick-prioritized [left right]
  (cond
    (nil? left) right
    (nil? right) left

    (and (displace? left)   ;; Pick the rightmost
         (displace? right)) ;; if both are marked as displaceable
    (with-meta* right
      (merge (meta* left) (meta* right)))

    (and (replace? left)    ;; Pick the rightmost
         (replace? right))  ;; if both are marked as replaceable
    (with-meta* right
      (merge (meta* left) (meta* right)))

    (or (displace? left)
        (replace? right))
    (with-meta* right
      (merge (-> left meta* (dissoc :displace))
             (-> right meta* (dissoc :replace))))

    (or (replace? left)
        (displace? right))
    (with-meta* left
      (merge (-> right meta* (dissoc :displace))
             (-> left meta* (dissoc :replace))))))

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
    (if (or (-> left meta :prepend)
            (-> right meta :prepend))
      (-> (into (empty left) (concat right left))
          (with-meta (merge (meta left)
                            (select-keys (meta right) [:displace]))))
      (into (empty left) (concat left right)))

    :else right))
