## 0.7.0-beta1 (2018-10-19)

* Fixed issue with multiple `:duct.profile/base` keys (duct-framework/duct#77)

## 0.7.0-alpha8 (2018-09-28)

* **BREAKING CHANGE** Removed `:duct.core/handler` key
* Updated Integrant to 0.7.0

## 0.7.0-alpha7 (2018-09-27)

* Fixed included configurations not having default tags (#16)

## 0.7.0-alpha6 (2018-09-22)

* Added `duct.core/resource` for better pretty-printing
* Added pretty-printing for `integrant.core/refset`

## 0.7.0-alpha5 (2018-05-29)

* Add boolean coercions to `#duct/env`
* Add `:duct/const` keyword

## 0.7.0-alpha4 (2018-04-10)

* Added `:duct.profile/local` to hierarchy
* Updated Integrant to 0.7.0-alpha2

## 0.7.0-alpha3 (2018-03-21)

* Add `prep-config` function
* Add `exec-config` function
* Add `:all` argument to `profile-keys`

## 0.7.0-alpha2 (2018-02-19)

* Ensured modules are applied after profiles

## 0.7.0-alpha1 (2018-02-07)

* **BREAKING CHANGE** Redesigned modules to use their own config
* **BREAKING CHANGE** Removed `prep` function
* **BREAKING CHANGE** Removed `exec` function
* **BREAKING CHANGE** Removed `:duct.core/include` key
* Added `build-config` function
* Added `fold-modules` function
* Added `await-daemons` function
* Added `:duct/profile` and child keys
* Added `#duct/include` reader tag
* Updated Integrant to 0.7.0-alpha1

## 0.6.2 (2017-12-17)

* Updated dependencies

## 0.6.1 (2017-08-24)

* Fixed `prep` not loading namespaces for keys added by modules

## 0.6.0 (2017-08-21)

* **BREAKING CHANGE** Removed implicit `prep` from `exec`
* Added `load-hierarchy`
* Added keys argument to `prep`
* Added data `:readers` option to `prep`
* Moved Fipp setup to `duct.core.repl`
* Updated Integrant to 0.6.1

## 0.5.2 (2017-07-30)

* Updated Integrant to 0.5.0

## 0.5.1 (2017-07-18)

* Updated Integrant to 0.4.1 to fix resume bug

## 0.5.0 (2017-06-23)

* **BREAKING CHANGE** Removed varargs from `read-config`
* Added custom data readers to `read-config`
* Added `:demote` and `:promote` metadata to `merge-configs`

## 0.4.0 (2017-06-02)

* **BREAKING CHANGE** Removed `duct.core/compile`
* **BREAKING CHANGE** `exec` initiates `:duct/daemon` keys by default
* Added `:duct/server` and `:duct/daemon` keys
* Added `keys` argument to `exec` function

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
