(ns com.fulcrologic.rad.rendering.semantic-ui.controls.action-button
  (:require
    [com.fulcrologic.rad.report :as report]
    [com.fulcrologic.rad.options-util :refer [?!]]
    [com.fulcrologic.rad.semantic-ui-options :as suo]
    [com.fulcrologic.fulcro.data-fetch :as df]
    [com.fulcrologic.rad.control :as control]
    #?(:cljs [com.fulcrologic.fulcro.dom :as dom]
       :clj  [com.fulcrologic.fulcro.dom-server :as dom])
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]))

(defsc ActionButton [_ {:keys [instance control-key]}]
  {:shouldComponentUpdate (fn [_ _ _] true)}
  (let [controls (control/component-controls instance)
        render   (suo/get-rendering-options instance suo/action-button-render)
        props    (comp/props instance)
        {:keys [label icon class action disabled? visible?] :as control} (get controls control-key)]
    (when control
      (let [label     (?! label instance)
            class     (?! class instance)
            loading?  (df/loading? (get-in props [df/marker-table (comp/get-ident instance)]))
            disabled? (or loading? (?! disabled? instance))
            visible?  (or (nil? visible?) (?! visible? instance))]
        (when visible?
          (or
            (?! render instance {:key       control-key
                                 :control   control
                                 :disabled? disabled?
                                 :loading?  loading?})
            (dom/button :.ui.tiny.primary.button
              {:key      (str control-key)
               :classes  [(when class class)]
               :disabled (boolean disabled?)
               :onClick  (fn [] (when action (action instance control-key)))}
              (when icon (dom/i {:className (str icon " icon")}))
              (when label label))))))))

(def render-control (comp/factory ActionButton {:keyfn :control-key}))
