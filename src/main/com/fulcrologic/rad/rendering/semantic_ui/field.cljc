(ns com.fulcrologic.rad.rendering.semantic-ui.field
  (:require
    [clojure.string :as str]
    [com.fulcrologic.fulcro.components :as comp]
    [com.fulcrologic.guardrails.core :refer [>defn =>]]
    #?(:cljs [com.fulcrologic.fulcro.dom :refer [div label input span]]
       :clj  [com.fulcrologic.fulcro.dom-server :refer [div label input span]])
    [com.fulcrologic.rad.attributes :as attr]
    [com.fulcrologic.fulcro.dom.html-entities :as ent]
    [com.fulcrologic.rad.options-util :refer [?!]]
    [com.fulcrologic.rad.form :as form]
    [taoensso.timbre :as log]))

(defn render-field-factory
  "Create a general field factory using the given input factory as the function to call to draw an input."
  ([input-factory]
   (render-field-factory {} input-factory))
  ([addl-props input-factory]
   (fn [env {::attr/keys [qualified-key] :as attribute}]
     (form/with-field-context [{:keys [value field-style-config
                                       visible? read-only?
                                       validation-message
                                       field-label invalid?]} (form/field-context env attribute)
                               addl-props (-> field-style-config
                                            (merge addl-props)
                                            (cond->
                                              read-only? (assoc :readOnly "readonly")))]
       (when visible?
         (div :.ui.field {:key     (str qualified-key)
                          :classes [(when invalid? "error")]}
           (label
             (or field-label (some-> qualified-key name str/capitalize))
             (when invalid? (str ent/nbsp "(" validation-message ")")))
           (div :.ui.input
             (input-factory (merge addl-props
                              {:value    value
                               :onBlur   (fn [v] (form/input-blur! env qualified-key v))
                               :onChange (fn [v] (form/input-changed! env qualified-key v))})))))))))
