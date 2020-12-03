(ns com.fulcrologic.rad.semantic-ui-options
  "Documented option keys for setting rendering-specific customization
  options when using Semantic UI Plugin as your DOM renderer.

  ALL options MUST appear under the rendering options key:

  ```
  (ns ...
    (:require
       [com.fulcrologic.rad.semantic-ui-options :as suo]
       ...))

  (defsc-report Report [this props]
    {suo/rendering-options { ... }}}
  ```

  Most of the options in this file can be given a global default using

  ```
  (set-global-rendering-options! fulcro-app options)
  ```

  where the `options` is a map of option keys/values.
  "
  (:require
    [com.fulcrologic.fulcro.components :as comp]))

(def rendering-options
  "Top-level key for specifying rendering options. All
   SUI customization options MUST appear under this key."
  ::rendering-options)

(def report-action-button-grouping
  "A string or `(fn [report-instance] string?)`.
   CSS class(es) to put in the div that surrounds the action buttons.

   Defaults to 'ui right floated buttons'."
  ::report-action-button-grouping)

(def report-row-button-grouping
  "A string or `(fn [report-instance] string?)`.
   CSS class(es) to put in the div that surrounds the action buttons on a table row.

   Defaults to 'ui buttons'."
  ::report-row-button-grouping)

(def report-row-button-renderer
  "A `(fn [instance row-props {:keys [key disabled?]}] dom-element)`.

  * `instance` - the report instance
  * `row-props` - the data props of the row
  * `key` - a unique key that can be used for react on the element.
  * `onClick` - a generated function according to the buton's action setting
  * `disabled?`-  true if the calculation of your disabled? option is true.

  Overrides the rendering of action button controls.

  You must return a DOM element to render for the control. If you return nil then
  the default (button) will be rendered."
  ::report-row-button-renderer)

(def action-button-render
  "A `(fn [instance {:keys [key control disabled? loading?]}] dom-element)`.

  * `key` - the key you used to add it to the controls list.
  * `control` - the map of options you gave for the control.
  * `disabled?`-  true if the calculation of your disabled? option is true.
  * `loading?` - true if the component is loading data.

  Overrides the rendering of action button controls.

  You must return a DOM element to render for the control. If you return nil then
  the default (button) will be rendered."
  ::action-button-render)

(def selectable-table-rows?
  "A boolean. When true the table will support click on a row to affix a highlight to that row."
  ::selectable-table-rows?)

(defn get-rendering-options
  "Get rendering options from a mounted component `c`.

   WARNING: If c is a class, then global overrides will not be honored."
  ([c & ks]
   (let [app            (comp/any->app c)
         global-options (some-> app
                          :com.fulcrologic.fulcro.application/runtime-atom
                          deref
                          ::rendering-options)
         options        (merge
                          global-options
                          (comp/component-options c ::rendering-options))]
     (if (seq ks)
       (get-in options (vec ks))
       options))))

(defn set-global-rendering-options!
  "Set rendering options on the application such that they serve as *defaults*.

  The `options` parameter to this function MUST NOT have the key suo/rendering-options, but
  should instead just have the parameters themselves (e.g. ::suo/action-button-renderer).
  "
  [app options]
  (swap! (:com.fulcrologic.fulcro.application/runtime-atom app)
    assoc
    ::rendering-options
    options))