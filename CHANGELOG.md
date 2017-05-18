## 0.3.3 (2017-05-18)

* Added fipp pprint protocol for Integrant refs
* Fixed bad require in `duct.core.merge`

## 0.3.2 (2017-05-09)

* Change `:duct.core/include` key to include configs recursively
* Added caching to `:duct.core/include`

## 0.3.1 (2017-05-08)

* Fixed typo in prepping `:duct.core/include` key

## 0.3.0 (2017-05-08)

* **BREAKING CHANGE** Removed `#duct/import` tag
* Added `:duct.core/include` key
* Updated Clojure version to 1.9.0-alpha16

## 0.2.3 (2017-05-06)

* Added `:duct.core/handler` key

## 0.2.2 (2017-05-05)

* Added `#duct/import` tag

## 0.2.1 (2017-04-25)

* Fixed `prep` when modules have dependencies

## 0.2.0 (2017-04-24)

* **BREAKING CHANGE** Changed module design to be implicitly ordered
* **BREAKING CHANGE** Added `duct` namespace to `resource` and `env` data readers
* Updated Integrant to 0.4.0

## 0.1.1 (2017-04-14)

* Added `:distinct` merge metadata

## 0.1.0 (2017-04-14)

* First release
