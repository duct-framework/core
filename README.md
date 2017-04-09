# Duct core

The core of the next iteration of the [Duct][] framework. It extends
the [Integrant][] micro-framework with support for modules, asset
compilation and environment variables.

[duct]:      https://github.com/duct-framework/duct
[integrant]: https://github.com/weavejester/integrant

## Installation

To install, add the following to your project `:dependencies`:

    [duct/core "0.9.0-SNAPSHOT"]

## Usage

First we need to read in an Integrant configuration from a file or
resource:

```clojure
(require '[clojure.java.io :as io]
         '[duct.core :as duct])

(defn get-config []
  (read-config (io/resource "example/config.edn")))
```

Once we have a configuration, we have three options. The first option
is to `prep` the configuration, which will load in all relevant
namespaces and apply all modules in the `:duct/modules` key.

This is ideally used with `integrant.repl`:

```clojure
(require '[integrant.repl :refer :all])

(set-prep! #(duct/prep (get-config)))
```

Alternatively, we can `compile` the configuration. This initiates all
keys inheriting from `duct.compiler`, effectively acting like an asset
pipeline. This is typically done before building an uberjar.

```clojure
(duct/compile (get-config))
```

Finally, we can `exec` the configuration. This prepares and initiates
the configuration, then blocks the current thread. This is designed to
be used from the `-main` function:

```clojure
(defn -main []
  (duct/exec (get-config)))
```

## License

Copyright © 2017 James Reeves

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
