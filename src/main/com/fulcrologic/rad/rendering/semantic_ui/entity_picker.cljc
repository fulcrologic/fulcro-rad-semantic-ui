(ns com.fulcrologic.rad.rendering.semantic-ui.entity-picker
  (:require
    #?(:cljs [com.fulcrologic.fulcro.dom :as dom :refer [div h3 button i span]]
       :clj
       [com.fulcrologic.fulcro.dom-server :as dom :refer [div h3 button i span]])
    [com.fulcrologic.fulcro.algorithms.form-state :as fs]
    [com.fulcrologic.fulcro.algorithms.normalized-state :as fns]
    [com.fulcrologic.fulcro.algorithms.tempid :as tempid]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.raw.components :as rc]
    [com.fulcrologic.fulcro-i18n.i18n :refer [tr]]
    [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
    [com.fulcrologic.fulcro.react.hooks :as hooks]
    [com.fulcrologic.fulcro.ui-state-machines :as uism]
    [com.fulcrologic.rad.attributes :as attr]
    [com.fulcrologic.rad.attributes-options :as ao]
    [com.fulcrologic.rad.form :as form]
    [com.fulcrologic.rad.form-options :as fo]
    [com.fulcrologic.rad.options-util :refer [?!]]
    [com.fulcrologic.rad.picker-options :as po]
    [com.fulcrologic.rad.rendering.semantic-ui.components :refer [ui-wrapped-dropdown]]
    [com.fulcrologic.rad.rendering.semantic-ui.form-options :as sufo]
    [com.fulcrologic.semantic-ui.modules.modal.ui-modal :refer [ui-modal]]
    [com.fulcrologic.semantic-ui.modules.modal.ui-modal-header :refer [ui-modal-header]]
    [com.fulcrologic.semantic-ui.modules.modal.ui-modal-content :refer [ui-modal-content]]
    [com.fulcrologic.semantic-ui.modules.modal.ui-modal-actions :refer [ui-modal-actions]]
    [com.fulcrologic.fulcro.ui-state-machines :as uism :refer [defstatemachine]]
    [taoensso.encore :as enc]
    [taoensso.timbre :as log]))

(declare CreationModal)

(defn- integrate-with-parent-form! [{:keys [state app]} {:keys [parent-registry-key parent-ident parent-relation-attribute ident]}]
  (when (and parent-ident parent-relation-attribute parent-registry-key ident)
    (enc/when-let [ParentForm      (comp/registry-key->class parent-registry-key)
                   parent-props    (fns/ui->props @state ParentForm parent-ident)
                   parent-relation (ao/qualified-key parent-relation-attribute)
                   many?           (= (ao/cardinality parent-relation-attribute) :many)]
      (if-not (tempid/tempid? (second ident))
        (do (fns/swap!-> state
                         (assoc-in (conj (comp/get-ident CreationModal {}) :ui/open?) false)
                         (update-in (conj parent-ident parent-relation)
                                    (if many? #((fnil conj []) % ident)
                                              (constantly ident))))
            (po/load-options! app ParentForm parent-props parent-relation-attribute
                              {:force-reload? true})
            (comp/transact! app [(fs/mark-complete! {:entity-ident parent-ident
                                                     :field parent-relation})]))
        (log/warn "Saving the new value for" parent-relation "returned OK from the server yet"
                  "the tempid in" ident "has not been remapped to a real one, indicating that the save failed")))))

(defmutation save-complete [{:keys [parent-registry-key parent-ident parent-relation-attribute ident] :as params}]
  (action [env]
    (integrate-with-parent-form! env params)))

(defmutation quick-add [{:keys [ident entity parent-registry-key parent-ident parent-relation-attribute] :as params}]
  (ok-action [{:keys [tempid->realid] :as env}]
    (let [[ident entity] (tempid/resolve-tempids [ident entity] tempid->realid)
          params (assoc params :ident ident :entity entity)]
      (if (-> ident second tempid/tempid?)
        (log/error "Quick add failed. Server may not have saved the data")
        (integrate-with-parent-form! env params))))
  (remote [env]
    (let [delta {ident (reduce-kv
                         (fn [m k v]
                           (assoc m k {:after v}))
                         {}
                         entity)}]
      (-> env
        (m/returning (rc/nc (vec (keys entity))))
        (m/with-server-side-mutation `form/save-as-form)
        (m/with-params {::form/master-pk (first ident)
                        ::form/id        (second ident)
                        ::form/delta     delta})))))

(defmutation start-modal-form [{:keys [ident parent-ident parent-registry-key parent-relation-attribute Form]}]
  (action [{:keys [app state]}]
    (if Form
      (let [id (second ident)]
        (swap! state update-in [:component/id ::CreationModal] assoc :ui/open? true :ui/form-props ident)
        (comp/set-query! app CreationModal {:query [:ui/open? {:ui/form-props (comp/get-query Form @state)}]})
        (form/start-form! app id Form {:embedded? true
                                       :on-saved  [(save-complete {:ident                     ident
                                                                   :parent-registry-key       parent-registry-key
                                                                   :parent-ident              parent-ident
                                                                   :parent-relation-attribute parent-relation-attribute})]}))
      (log/error "Cannot create/edit. No form supplied."))))

(defsc CreationModal [this {:ui/keys [open? form-props] :as props} {::form/keys [parent-relation parent]
                                                                    :keys       [Form]}]
  {:query         [:ui/open?
                   :ui/form-props]
   :ident         (fn [] [:component/id ::CreationModal])
   :initial-state {}}
  (let [title (?! (get-in (comp/component-options parent) [fo/field-options parent-relation fo/title])
                parent form-props)] ; sending in parent instead of "this form" b/c it doesn't exist yet
    (ui-modal {:open open?}
      (when (seq title) (ui-modal-header {} title
                          (dom/div :.ui.right.floated.button
                            {:onClick (fn [] (uism/trigger! this (comp/get-ident Form form-props) :event/save {}))}
                            "OK")
                          (dom/div :.ui.right.floated.button
                            {:onClick (fn [] (m/set-value!! this :ui/open? false))}
                            "Cancel")))
      (ui-modal-content {}
        (when Form
          (let [factory (comp/computed-factory Form)]
            (when (and (map? form-props) (seq form-props))
              (factory form-props {fo/show-header? false}))))))))

(def ui-creation-modal (comp/computed-factory CreationModal))

(defsc CreationContainer [this pass-through-props]
  {:use-hooks? true}
  (let [app   (comp/any->app this)
        props (hooks/use-component app CreationModal {:initialize?    true
                                                      :keep-existing? true})]
    (when props
      (ui-creation-modal props pass-through-props))))

(def ui-creation-container (comp/computed-factory CreationContainer))

(defsc ToOnePicker [this {:keys [env attr]}]
  {:componentDidMount     (fn [this]
                            (let [{:keys [env attr]} (comp/props this)
                                  form-instance (::form/form-instance env)
                                  props         (comp/props form-instance)
                                  form-class    (comp/react-type form-instance)]
                              (po/load-options! form-instance form-class props attr)))
   :shouldComponentUpdate (fn [] true)}
  (let [{::form/keys [master-form form-instance]} env
        visible? (form/field-visible? form-instance attr)]
    (when visible?
      (let [{::form/keys [field-options]} (comp/component-options form-instance)
            {::attr/keys [qualified-key required?]} attr
            field-options (get field-options qualified-key)
            target-id-key (ao/target attr)
            {Form      ::po/form
             ::po/keys [quick-create allow-edit? allow-create? cache-key query-key]} (merge attr field-options)
            props         (comp/props form-instance)
            cache-key     (or (?! cache-key (comp/react-type form-instance) props) query-key)
            cache-key     (or cache-key query-key (log/error "Ref field MUST have either a ::picker-options/cache-key or ::picker-options/query-key in attribute " qualified-key))
            options       (get-in props [::po/options-cache cache-key :options])
            value         [target-id-key (get-in props [qualified-key target-id-key])]
            field-label   (form/field-label env attr)
            read-only?    (or (form/read-only? master-form attr) (form/read-only? form-instance attr))
            invalid?      (and (not read-only?) (form/invalid-attribute-value? env attr))
            extra-props   (cond-> (?! (form/field-style-config env attr :input/props) env)
                            quick-create (merge {:allowAdditions   true
                                                 :additionPosition "top"
                                                 :onAddItem        (fn [_ data]
                                                                     #?(:cljs
                                                                        (try
                                                                          (let [v      (.-value ^js data)
                                                                                entity (quick-create v)
                                                                                id     (get entity target-id-key)
                                                                                ident  [target-id-key id]]
                                                                            (when (tempid/tempid? id)
                                                                              (comp/transact! form-instance
                                                                                [(quick-add {:parent-ident              (comp/get-ident form-instance)
                                                                                             :parent-registry-key       (comp/class->registry-key (comp/get-class form-instance))
                                                                                             :parent-relation-attribute attr
                                                                                             :ident                     ident
                                                                                             :entity                    entity})])))
                                                                          (catch :default e
                                                                            (log/error e "Quick create failed.")))))}))
            top-class     (sufo/top-class form-instance attr)
            can-edit?     (?! allow-edit? form-instance qualified-key)
            can-create?   (if-some [v (?! allow-create? form-instance qualified-key)] v (boolean Form))
            mutable?      (and Form (or can-edit? can-create?))
            onSelect      (fn [v] (form/input-changed! env qualified-key v))]
        (div {:className (or top-class "ui field")
              :classes   [(when invalid? "error")]}
          (dom/label field-label (when invalid? (str " (" (tr "Required") ")")))
          (if read-only?
            (let [value (first (filter #(= value (:value %)) options))]
              (:text value))
            (comp/fragment {}                               ; FIXME: CSS help pls
              (ui-wrapped-dropdown (merge extra-props
                                     {:onChange  (fn [v] (onSelect v))
                                      :compact   true
                                      :value     value
                                      :clearable (not required?)
                                      :disabled  read-only?
                                      :options   options}))
              (when mutable?
                (let [open-editor! (fn [id]
                                     #?(:cljs (comp/transact! this
                                                [(start-modal-form {:parent-ident              (comp/get-ident form-instance)
                                                                    :parent-registry-key       (comp/class->registry-key (comp/get-class form-instance))
                                                                    :parent-relation-attribute attr
                                                                    :Form                      Form
                                                                    :ident                     [target-id-key id]})])))]
                  (comp/fragment {}
                    (ui-creation-container (merge env
                                             {::form/parent          form-instance
                                              ::form/parent-relation qualified-key
                                              :Form                  Form}))
                    (when can-create?
                      (dom/button :.ui.basic.icon.button
                        {:onClick (fn [] (open-editor! (tempid/tempid)))}
                        (dom/i :.plus.icon)))
                    (when can-edit?
                      (dom/button :.ui.basic.icon.button
                        {:disabled (not (second value))
                         :onClick  (fn []
                                     (when-let [id (some-> value second)]
                                       (open-editor! id)))}
                        (dom/i :.pencil.icon)))))))))))))

(let [ui-to-one-picker (comp/factory ToOnePicker {:keyfn (fn [{:keys [attr]}] (::attr/qualified-key attr))})]
  (defn to-one-picker [env attribute]
    (ui-to-one-picker {:env  env
                       :attr attribute})))

(defsc ToManyPicker [this {:keys [env attr]}]
  {:componentDidMount (fn [this]
                        (let [{:keys [env attr]} (comp/props this)
                              form-instance (::form/form-instance env)
                              props         (comp/props form-instance)
                              form-class    (comp/react-type form-instance)]
                          (po/load-options! form-instance form-class props attr)))}
  (let [{::form/keys [form-instance]} env
        visible? (form/field-visible? form-instance attr)]
    (when visible?
      (let [{::form/keys [attributes field-options]} (comp/component-options form-instance)
            {attr-field-options ::form/field-options
             ::attr/keys        [qualified-key]} attr
            field-options      (get field-options qualified-key)
            target-id-key      (first (keep (fn [{k ::attr/qualified-key ::attr/keys [target]}]
                                              (when (= k qualified-key) target)) attributes))
            {:keys     [style]
             Form      ::po/form
             ::po/keys [quick-create allow-create? allow-edit? cache-key query-key]} (merge attr-field-options field-options)
            cache-key          (or (?! cache-key (comp/react-type form-instance) (comp/props form-instance)) query-key)
            cache-key          (or cache-key query-key (log/error "Ref field MUST have either a ::picker-options/cache-key or ::picker-options/query-key in attribute " qualified-key))
            props              (comp/props form-instance)
            options            (get-in props [::po/options-cache cache-key :options])
            extra-props        (cond-> (?! (form/field-style-config env attr :input/props) env)
                                       (and (= style :dropdown) quick-create)
                                       (merge {:allowAdditions   true
                                               :additionPosition "top"
                                               :onAddItem        (fn [_ data]
                                                                   #?(:cljs
                                                                      (try
                                                                        (let [v      (.-value ^js data)
                                                                              entity (quick-create v)
                                                                              id     (get entity target-id-key)
                                                                              ident  [target-id-key id]]
                                                                          (when (tempid/tempid? id)
                                                                            (comp/transact! form-instance
                                                                                            [(quick-add {:parent-ident              (comp/get-ident form-instance)
                                                                                                         :parent-registry-key       (comp/class->registry-key (comp/get-class form-instance))
                                                                                                         :parent-relation-attribute attr
                                                                                                         :ident                     ident
                                                                                                         :entity                    entity})])))
                                                                        (catch :default e
                                                                          (log/error e "Quick create failed.")))))}))
            current-selection  (into #{}
                                     (keep (fn [entity]
                                             (when-let [id (get entity target-id-key)]
                                               [target-id-key id])))
                                     (get props qualified-key))
            field-label        (form/field-label env attr)
            invalid?           (form/invalid-attribute-value? env attr)
            read-only?         (form/read-only? form-instance attr)
            top-class          (sufo/top-class form-instance attr)
            can-create?        (if-some [v (?! allow-create? form-instance qualified-key)] v (boolean Form))
            mutable?           (and Form can-create?)
            validation-message (when invalid? (form/validation-error-message env attr))]
        (div {:className (or top-class "ui field")
              :classes   [(when invalid? "error")]}
          (dom/label field-label " " (when invalid? validation-message))
          (div :.ui.middle.aligned.celled.list.big
            {:style {:marginTop "0"}}
            (if (= style :dropdown)
              (ui-wrapped-dropdown
                (merge extra-props
                  {:value    current-selection
                   :multiple true
                   :disabled read-only?
                   :options  options
                   :onChange (fn [v] (form/input-changed! env qualified-key v))}))
              (map (fn [{:keys [text value]}]
                     (let [checked? (contains? current-selection value)]
                       (div :.item {:key value}
                         (div :.content {}
                           (div :.ui.toggle.checkbox {:style {:marginTop "0"}}
                             (dom/input
                               (merge extra-props
                                 {:type     "checkbox"
                                  :checked  checked?
                                  :onChange #(if-not checked?
                                               (form/input-changed! env qualified-key (vec (conj current-selection value)))
                                               (form/input-changed! env qualified-key (vec (disj current-selection value))))}))
                             (dom/label text))))))
                options))
               (when mutable?
                 (let [open-editor! (fn [id]
                                      #?(:cljs (comp/transact! this
                                                               [(start-modal-form {:parent-ident              (comp/get-ident form-instance)
                                                                                   :parent-registry-key       (comp/class->registry-key (comp/get-class form-instance))
                                                                                   :parent-relation-attribute attr
                                                                                   :Form                      Form
                                                                                   :ident                     [target-id-key id]})])))]
                   (comp/fragment {}
                                  (ui-creation-container (merge env
                                                                {::form/parent          form-instance
                                                                 ::form/parent-relation qualified-key
                                                                 :Form                  Form}))
                                  (when can-create?
                                    (dom/button :.ui.basic.icon.button
                                                {:onClick (fn [] (open-editor! (tempid/tempid)))}
                                                (dom/i :.plus.icon))))))))))))

(def ui-to-many-picker (comp/factory ToManyPicker {:keyfn :id}))
(let [ui-to-many-picker (comp/factory ToManyPicker {:keyfn (fn [{:keys [attr]}] (::attr/qualified-key attr))})]
  (defn to-many-picker [env attribute]
    (ui-to-many-picker {:env  env
                        :attr attribute})))
