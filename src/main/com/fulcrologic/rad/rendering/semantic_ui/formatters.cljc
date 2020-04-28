(ns com.fulcrologic.rad.rendering.semantic-ui.formatters
  "Formatters for read-only values, such as in reports."
  (:require
    [com.fulcrologic.rad.type-support.decimal :as math]))

;; TASK: More default type formatters
(defn inst->human-readable-date
  "Converts a UTC Instant into the correctly-offset and human-readable (e.g. America/Los_Angeles) date string."
  [inst]
  #?(:cljs
     (when (inst? inst)
       (.toLocaleDateString ^js inst js/undefined #js {:weekday "short" :year "numeric" :month "short" :day "numeric"}))))

(def numeric->str math/numeric->str)
