(ns com.fulcrologic.rad.rendering.semantic-ui.container
  (:require
    #?@(:cljs
        [[com.fulcrologic.fulcro.dom :as dom :refer [div]]]
        :clj
        [[com.fulcrologic.fulcro.dom-server :as dom :refer [div]]])
    [com.fulcrologic.fulcro.components :as comp]
    [com.fulcrologic.rad.container :as container]))

(defn render-container-layout [container-instance]
  (let [{::container/keys [children layout]} (comp/component-options container-instance)]
    ;; TODO: Layout
    (let [container-props (comp/props container-instance)]
      (map-indexed
        (fn [idx cls]
          (let [k       (comp/class->registry-key cls)
                factory (comp/factory cls)
                props   (get container-props k {})]
            (dom/div {:key idx}
              (factory props)))) children))))
