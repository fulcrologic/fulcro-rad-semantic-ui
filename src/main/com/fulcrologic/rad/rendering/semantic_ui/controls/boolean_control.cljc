(ns com.fulcrologic.rad.rendering.semantic-ui.controls.boolean-control
  (:require
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.ui-state-machines :as uism]
    [com.fulcrologic.rad.options-util :refer [?!]]
    [com.fulcrologic.rad.report :as report]
    [taoensso.timbre :as log]
    #?(:cljs [com.fulcrologic.fulcro.dom :as dom]
       :clj  [com.fulcrologic.fulcro.dom-server :as dom])
    [com.fulcrologic.rad.control :as control]))

(defsc BooleanControl [_ {:keys [instance control-key]}]
  {:shouldComponentUpdate (fn [_ _ _] true)}
  (let [controls (control/component-controls instance)
        {:keys [label onChange disabled? visible?] :as control} (get controls control-key)]
    (if control
      (let [label     (or (?! label instance))
            disabled? (?! disabled? instance)
            visible?  (or (nil? visible?) (?! visible? instance))
            value     (control/current-value instance control-key)]
        (when visible?
          (dom/div :.field
            (dom/label label)
            (dom/div :.ui.fitted.toggle.checkbox {:key (str control-key)}
              (dom/input {:type     "checkbox"
                          :readOnly (boolean disabled?)
                          :onChange (fn [_]
                                      (control/set-parameter! instance control-key (not value))
                                      (when onChange
                                        (onChange instance (not value))))
                          :checked  (boolean value)})
                (dom/label "")))))
      (log/error "Could not find control definition for " control-key))))

(def render-control (comp/factory BooleanControl {:keyfn :control-key}))
