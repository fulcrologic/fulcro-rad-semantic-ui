(ns com.fulcrologic.rad.rendering.semantic-ui.container
  (:require
    #?@(:cljs
        [[com.fulcrologic.fulcro.dom :as dom :refer [div]]]
        :clj
        [[com.fulcrologic.fulcro.dom-server :as dom :refer [div]]])
    [com.fulcrologic.fulcro.components :as comp]
    [com.fulcrologic.rad.container :as container]
    [com.fulcrologic.rad.control :as control]
    [com.fulcrologic.rad.options-util :refer [?!]]
    [com.fulcrologic.rad.rendering.semantic-ui.form :as sui-form]
    [taoensso.timbre :as log]))

(comp/defsc StandardContainerControls [_ {:keys [instance]}]
  {:shouldComponentUpdate (fn [_ _ _] true)}
  (let [{:keys [::control/controls ::control/control-layout]} (comp/component-options instance)
        {:keys [action-buttons inputs]} control-layout]
    (let [controls       (merge (container/shared-controls instance) controls)
          action-buttons (or action-buttons
                           (keep (fn [[k v]] (when (and
                                                     (not (:local? v))
                                                     (= :button (:type v))) k)) controls))
          inputs         (or inputs
                           (vector (into [] (keep
                                              (fn [[k v]] (when-not (or
                                                                      (:local? v)
                                                                      (= :button (:type v))) k))
                                              controls))))]
      (div :.ui.top.attached.compact.segment
        (dom/h3 :.ui.header
          (or (some-> instance comp/component-options ::container/title (?! instance)) "")
          (div :.ui.right.floated.buttons
            (keep (fn [k] (control/render-control instance k (get controls k))) action-buttons)))
        (div :.ui.form
          (map-indexed
            (fn [idx row]
              (div {:key idx :className (sui-form/n-fields-string (count row))}
                (map #(if-let [c (get controls %)]
                        (control/render-control instance % c)
                        (dom/div :.ui.field {:key (str %)} "")) row)))
            inputs))))))

(let [ui-standard-container-controls (comp/factory StandardContainerControls)]
  (defn render-standard-controls [instance]
    (ui-standard-container-controls {:instance instance})))

(defn render-container-layout [container-instance]
  (let [{::container/keys [children layout]} (comp/component-options container-instance)]
    ;; TODO: Layout. Custom controls rendering as a separate config?
    (let [container-props (comp/props container-instance)]
      (dom/div :.ui.segments
        (render-standard-controls container-instance)
        (dom/div :.ui.attached.segment
          (map-indexed
            (fn [idx cls]
              (let [k       (comp/class->registry-key cls)
                    factory (comp/computed-factory cls)
                    props   (get container-props k {})]
                (dom/div {:key idx}
                  (factory props {::container/controlled? true})))) children))))))
