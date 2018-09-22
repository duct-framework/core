(ns duct.core.resource
  (:require [clojure.java.io :as io]))

(defrecord Resource [path url]
  Object
  (toString [_] (str "#duct/resource \"" path "\""))
  
  io/Coercions
  (as-file [_] (io/as-file url))
  (as-url  [_] url)
  
  io/IOFactory
  (make-reader [_ opts] (io/make-reader (io/make-input-stream url opts) opts))
  (make-writer [_ opts] (io/make-writer (io/make-output-stream url opts) opts))
  (make-input-stream  [_ opts] (io/make-input-stream url opts))
  (make-output-stream [r opts]
    (throw (IllegalArgumentException.
            (str "Cannot open <" r "> as an OutputStream.")))))

(defmethod print-method Resource [r ^java.io.Writer w]
  (.write w (str r)))

(defn make-resource [path]
  (Resource. path (io/resource path)))
