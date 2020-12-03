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

(defn get-rendering-options
  ([c k]
   (get-in (comp/component-options c) [::rendering-options k]))
  ([c]
   (::rendering-options (comp/component-options c))))
