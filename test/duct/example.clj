(ns duct.example
  (:require [integrant.core :as ig]))

(defmethod ig/init-key ::foo [_ x] [:foo x])
