(ns com.fulcrologic.rad.rendering.semantic-ui.boolean-field
  (:require
    #?(:cljs
       [com.fulcrologic.fulcro.dom :as dom :refer [div label input]]
       :clj
       [com.fulcrologic.fulcro.dom-server :as dom :refer [div label input]])
    [com.fulcrologic.rad.attributes :as attr]
    [com.fulcrologic.fulcro.components :as comp]
    [clojure.string :as str]
    [com.fulcrologic.rad.form :as form]
    [com.fulcrologic.rad.options-util :refer [?!]]
    [com.fulcrologic.fulcro.dom.events :as evt]))

(defn render-field [{::form/keys [form-instance] :as env} attribute]
  (let [k           (::attr/qualified-key attribute)
        props       (comp/props form-instance)
        user-props  (?! (form/field-style-config env attribute :input/props) env)
        field-label (form/field-label env attribute)
        read-only?  (form/read-only? form-instance attribute)
        value       (get props k false)]
    (div :.ui.field {:key (str k)}
      (div :.ui.checkbox
        (input (merge
                 {:checked  value
                  :type     "checkbox"
                  :disabled (boolean read-only?)
                  :onChange (fn [evt]
                              (let [v (not value)]
                                (form/input-blur! env k v)
                                (form/input-changed! env k v)))}
                 user-props))
        (label field-label)))))

