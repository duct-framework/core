(ns duct.core.repl
  (:require [duct.core.resource :as resource]
            [fipp.ednize :as fipp]
            [integrant.core :as ig]))

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
