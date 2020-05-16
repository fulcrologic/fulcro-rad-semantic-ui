(ns com.fulcrologic.rad.rendering.semantic-ui.controls.control
  (:require
    [clojure.string :as str]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.rad.options-util :refer [?!]]
    [com.fulcrologic.guardrails.core :refer [>defn =>]]
    [com.fulcrologic.rad.report :as report]
    #?(:cljs [com.fulcrologic.fulcro.dom :as dom]
       :clj  [com.fulcrologic.fulcro.dom-server :as dom])
    [com.fulcrologic.rad.attributes :as attr]
    [taoensso.timbre :as log]))

(defsc Control [_ {:keys [report-instance control-key input-factory] :as report-env}]
  {:shouldComponentUpdate (fn [_ _ _] true)}
  (let [{:keys [:com.fulcrologic.rad.control/controls]} (comp/component-options report-instance)
        props (comp/props report-instance)
        {:keys [label onChange disabled? visible? user-props] :as control} (get controls control-key)]
    (if (and input-factory control)
      (let [label     (or (?! label report-instance))
            disabled? (?! disabled? report-instance)
            visible?  (or (nil? visible?) (?! visible? report-instance))
            value     (get-in props [:ui/parameters control-key])
            onChange  (fn [new-value]
                        (report/set-parameter! report-instance control-key new-value)
                        (when onChange
                          (onChange report-instance new-value)))]
        (when visible?
          (dom/div :.ui.field {:key (str control-key)}
            (dom/label label)
            (input-factory report-env (merge user-props
                                        {:disabled? disabled?
                                         :value     value
                                         :onChange  onChange})))))
      (log/error "Cannot render control. Missing input factory or control definition."))))

(def ui-control (comp/factory Control {:keyfn :control-key}))
