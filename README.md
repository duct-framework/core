# Duct core

[![Build Status](https://travis-ci.org/duct-framework/core.svg?branch=master)](https://travis-ci.org/duct-framework/core)

The core of the next iteration of the [Duct][] framework. It extends
the [Integrant][] micro-framework with support for modules, asset
compilation and environment variables.

[duct]:      https://github.com/duct-framework/duct
[integrant]: https://github.com/weavejester/integrant

## Installation

To install, add the following to your project `:dependencies`:

    [duct/core "0.8.0"]

## Usage

First we need to read in an Integrant configuration from a file or
resource:

```clojure
(require '[clojure.java.io :as io]
         '[duct.core :as duct])

(defn get-config []
  (duct/read-config (io/resource "example/config.edn")))
```

Once we have a configuration, we have three options. The first option
is to `prep-config` the configuration, which will load in all relevant
namespaces and apply all modules.

This is ideally used with `integrant.repl`:

```clojure
(require '[integrant.repl :refer :all])

(set-prep! #(duct/prep-config (get-config)))
```

Alternatives we can `prep-config` then `exec-config` the configuration. This
initiates the configuration, then blocks the current thread if the
system includes any keys deriving from `:duct/daemon`. This is
designed to be used from the `-main` function:

```clojure
(defn -main []
  (-> (get-config) (duct/prep-config) (duct/exec-config)))
```

You can change the executed keys to anything you want by adding in an
additional argument. This is frequently used with the `parse-keys`
function, which parses keywords from command-line arguments:

```clojure
(defn -main [& args]
  (let [keys (or (duct/parse-keys args) [:duct/daemon])]
    (-> (get-config)
        (duct/prep-config keys)
        (duct/exec-config keys))))
```

This allows other parts of the system to be selectively executed. For
example:

```
lein run :duct/compiler
```

Would initiate all the compiler keys. And:

```
lein run :duct/migrator
```

Would initiate the migrator and run all pending migrations. If no
arguments are supplied, the keys default to `[:duct/daemon]` in this
example.

## Keys

This library introduces four Integrant keys.

`:duct.core/environment` specifies the environment of the
configuration, and may be set to `:development` or `:production`. It
does nothing on its own, but may be used by modules.

`:duct.core/project-ns` specifies the base namespace of your
project. This is often used by modules for determining where to put
things. For example, public web resources are typically placed in the
`resources/<project-ns>/public` directory.

`:duct.core/handler` should be configured with a map with two keys:
`:router`, which should be a Ring handler, and `:middleware`, which
should be an ordered vector of middleware. The middleware is applied
to the router to create a completed Ring handler.

In addition, it sets up some deriviations:

* `:duct.server/http` derives from `:duct/server`
* `:duct/server` derives from `:duct/daemon`

## Modules

Modules are Integrant keywords that derive from `:duct/module`, and
initiate into maps with two keys: `:req` and `:fn`. The `:req` key is
optional, and should contain a collection of keys that are required to
be present in the map. The `:fn` key is a pure function that
transforms the configuration into a new configuration.

The `:fn` **must** be pure, and **must never** remove top-level keys
from the configuration. A module should add functionality to a
configuration; it should not override or remove existing functionality
supplied by the user.

Here's an example module:

```clojure
(require '[integrant.core :as ig])

(derive :duct.module/example :duct/module)

(defmethod ig/init-key :duct.module/example [_ port]
  {:req #{:duct.server.http/jetty}
   :fn  (fn [config]
          (assoc-in config [:duct.server.http/jetty :port] port))})
```

This above module updates the port number of the `:duct.server.http/jetty`
key. Note that this key is a requirement; we need it to exist for the
module to run. The module requirements are used for ordering modules,
and for ensuring their basic pre-requisites are met.

In the previous example we used `assoc-in`, but the `duct.core`
namespace also has a `merge-configs` function we can use to achieve a
similar result in a smarter way:

```clojure
(require '[duct.core.merge :as merge])

(defmethod ig/init-key :duct.module/example [_ port]
  {:req #{:duct.server/http}
   :fn  (fn [config]
          (duct/merge-configs
           config
           {:duct.server/http {:port (merge/displace port)}}))})
```

In this example we've changed the requirement from
`:duct.server.http/jetty` to the more generic `:duct.server/http`,
which the latter derives from. The `merge-configs` function is smart
enough to merge `:duct.server/http` into a more specific derived key,
if one exists.

We've also added merge metadata using `merge/displace`. This tells
`merge-configs` not to override the port if it already exists in the
configuraton.

## Documentation

* [API Docs](https://duct-framework.github.io/core/index.html)

## License

Copyright © 2017 James Reeves

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
