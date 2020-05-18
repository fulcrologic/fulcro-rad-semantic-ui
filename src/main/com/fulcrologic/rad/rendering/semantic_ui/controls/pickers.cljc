(ns com.fulcrologic.rad.rendering.semantic-ui.controls.pickers
  (:require
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.ui-state-machines :as uism]
    [com.fulcrologic.rad.rendering.semantic-ui.components :refer [ui-wrapped-dropdown]]
    [com.fulcrologic.rad.options-util :refer [?!]]
    [com.fulcrologic.rad.report :as report]
    [taoensso.timbre :as log]
    #?(:cljs [com.fulcrologic.fulcro.dom :as dom]
       :clj  [com.fulcrologic.fulcro.dom-server :as dom])))

(defsc SimplePicker [_ {:keys [instance control-key]}]
  {:shouldComponentUpdate (fn [_ _ _] true)}
  (let [{:keys [:com.fulcrologic.rad.control/controls]} (comp/component-options instance)
        props (comp/props instance)
        {:keys [label onChange disabled? visible? action placeholder options user-props] :as control} (get controls control-key)]
    (when control
      (let [label       (or (?! label instance))
            disabled?   (?! disabled? instance)
            placeholder (?! placeholder instance)
            visible?    (or (nil? visible?) (?! visible? instance))
            value       (get-in props [:ui/parameters control-key])]
        (when visible?
          (dom/div :.ui.field {:key (str control-key)}
            (dom/label label)
            (ui-wrapped-dropdown (merge
                                   user-props
                                   {:disabled    disabled?
                                    :placeholder (str placeholder)
                                    :options     options
                                    :value       value
                                    :onChange    (fn [v]
                                                   (uism/trigger! instance (comp/get-ident instance) :event/set-parameter {control-key v})
                                                   (binding [comp/*after-render* true]
                                                     (when onChange
                                                       (onChange instance v))
                                                     (when action
                                                       (action instance))))}))))))))

(def render-control (comp/factory SimplePicker {:keyfn :control-key}))

