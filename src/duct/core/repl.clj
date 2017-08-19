(ns duct.core.repl
  (:require [fipp.ednize :as fipp]
            [integrant.core :as ig]))

(extend-type integrant.core.Ref
  fipp/IOverride
  fipp/IEdn
  (-edn [r] (tagged-literal 'ig/ref (:key r))))
