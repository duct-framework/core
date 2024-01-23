# Duct core

[![Build Status](https://travis-ci.org/duct-framework/core.svg?branch=master)](https://travis-ci.org/duct-framework/core)

The core of the next iteration of the [Duct][] framework. It extends
the [Integrant][] micro-framework with support for modules, asset
compilation and environment variables.

[duct]:      https://github.com/duct-framework/duct
[integrant]: https://github.com/weavejester/integrant

## Installation

To install, add the following to your project `:dependencies`:

    [duct/core "0.8.1"]

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

Alternatively we can `prep-config` then `exec-config` the configuration. This
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

This library introduces a number of Integrant components:

* `:duct/const` is a component that returns its value when initialized.
* `:duct/module` denotes a Duct module (see the modules section).
* `:duct/profile` denotes a Duct profile (see the profiles section).
* `:duct.core/environment` specifies the environment of the
  configuration, and may be set to `:development` or `:production`. It
  does nothing on its own, but may be used by modules.
* `:duct.core/project-ns` specifies the base namespace of your
  project. This is often used by modules for determining where to put
  things. For example, public web resources are typically placed in the
  `resources/<project-ns>/public` directory.

## Readers

This library also introduces five new reader tags that can be used in
Duct configurations:

* `#duct/env "VARNAME"` allows an environment variable to be
  referenced in the configuration.
* `#duct/include "path"` will include replace the tag with the
  resource at the specified path. This allows a configuration to be
  split up into separate files.
* `#duct/resource "path"` will convert a resource path into a URL
* `#duct/displace` is equivalent to `^:displace`, but works with
  primitive values
* `#duct/replace` is equivalent to `^:replace`, but works with
  primitive values

## Modules

Modules are Integrant components that initialize into a pure
function. This function expects a configuration as its argument, and
returns a modified configuration.

Most modules derive from `:duct/module`. This both identifies them,
and ensures they are executed after profiles.

Here's a simple example module:

```clojure
(require '[integrant.core :as ig])

(derive :duct.module/example :duct/module)

(defmethod ig/init-key :duct.module/example [_ {:keys [port]}]
  (fn [config]
    (assoc-in config [:duct.server.http/jetty :port] port)))
```

This above module updates the port number of the `:duct.server.http/jetty`
key. By itself this isn't hugely useful, but modules can be made to
update many different components at once.

Modules can also have dependencies, achieved using
`integrant.core/prep-key`:

```clojure
(defmethod ig/prep-key :duct.module/example [_ options]
  (assoc options ::requires (ig/ref :duct.module/parent)))
```

This adds a reference to the module's options, ensuring that Integrant
will initialize the `:duct.module/parent` module before
`:duct.module/example`.

You can also have optional dependencies with `integrant.core/refset`:

```clojure
(defmethod ig/prep-key :duct.module/example [_ options]
  (assoc options ::requires (ig/refset #{:duct.module/parent})))
```

## Profiles

A profile is (currently) a type of module that merges the value of the
key into the resulting configuration.

There are five profile keys included in this library:

* `:duct.profile/base` is the base of all new profiles
* `:duct.profile/dev` is a profile for development
* `:duct.profile/local` is a profile only on your local development
  machine
* `:duct.profile/test` is a profile for testing
* `:duct.profile/prod` is a profile for production

## Documentation

* [API Docs](https://duct-framework.github.io/core/index.html)

## License

Copyright © 2020 James Reeves

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
