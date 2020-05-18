(ns com.fulcrologic.rad.rendering.semantic-ui.controls.control
  (:require
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.ui-state-machines :as uism]
    [com.fulcrologic.rad.options-util :refer [?!]]
    [com.fulcrologic.guardrails.core :refer [>defn =>]]
    [com.fulcrologic.rad.report :as report]
    #?(:cljs [com.fulcrologic.fulcro.dom :as dom]
       :clj  [com.fulcrologic.fulcro.dom-server :as dom])
    [taoensso.timbre :as log]))

(defsc Control [_ {:keys [instance control control-key input-factory] :as report-env}]
  {:shouldComponentUpdate (fn [_ _ _] true)}
  (let [{:keys [:com.fulcrologic.rad.control/controls]} (comp/component-options instance)
        props (comp/props instance)
        {:keys [label onChange disabled? visible? user-props] :as control} (get controls control-key control)]
    (if (and input-factory control)
      (let [label     (or (?! label instance))
            disabled? (?! disabled? instance)
            visible?  (or (nil? visible?) (?! visible? instance))
            value     (get-in props [:ui/parameters control-key])
            onChange  (fn [new-value]
                        (uism/trigger! instance (comp/get-ident instance) :event/set-parameter {control-key new-value})
                        (when onChange
                          (onChange instance new-value)))]
        (when visible?
          (dom/div :.ui.field {:key (str control-key)}
            (dom/label label)
            (input-factory report-env (merge user-props
                                        {:disabled? disabled?
                                         :value     value
                                         :onChange  onChange})))))
      (log/error "Cannot render control. Missing input factory or control definition."))))

(def ui-control (comp/factory Control {:keyfn :control-key}))
