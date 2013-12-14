(ns todomvc.item
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [>! <! put! alts!]]
            [todomvc.utils :refer [now]]
            [clojure.string :as string]
            [om.core :as om]
            [om.dom :as dom :include-macros true]))

(def ESCAPE_KEY 27)
(def ENTER_KEY 13)

;; =============================================================================
;; Todo Item

(defn handle-submit [e todo {:keys [owner chans]}]
  (let [val (.trim (dom/get-node todo "editText"))]
    (if-not (string/blank? val)
      (go
        (>! (:on-save chans) [todo val])
        (om/replace! todo :title (:edit-text todo)))
      (put! (:on-destroy chans) (:id todo)))
    false))

(defn handle-edit [e todo {:keys [owner chans]}]
  (put! (:on-edit chans) todo)
  (let [node (dom/get-node owner "editField")]
    (.focus node)
    (.setSelectionRange (.. node -value -length) (.. node -value -length)))
  (om/replace! todo :edit-text (:title todo)))

(defn handle-key-down [e todo opts]
  (if (identical? (.-keyCode e) ESCAPE_KEY)
    (om/replace! todo :edit-text (:title todo))
    (handle-submit e todo opts)))

(defn handle-change [e todo]
  (om/replace! todo :edit-text (.. e -target -value)))

(defn todo-item [{:keys [completed editing] :as todo} chans]
  (reify
    dom/IRender
    (-render [_ owner]
      (let [m {:owner owner :chans chans}]
        (dom/li #js {:className (str (and completed "completed") " "
                                  (and editing "editing"))}
          (dom/div #js {:className "view"}
            (dom/input #js {:className "toggle"
                            :type "checkbox"
                            :checked (and completed "checked")
                            :onChange (fn [_] (put! (:toggle chans) todo))})
            (dom/label #js {:onDoubleClick #(handle-edit % todo owner)}
              (:title todo))
            (dom/button #js {:className "destroy"
                             :onClick (fn [_] (put! (:delete chans) todo))})
            (dom/input #js {:ref "editField"
                            :className "edit"
                            :value (:edit-text todo)
                            :onBlur #(handle-submit % todo m)
                            :onChange #(handle-change % todo)
                            :onKeyDown #(handle-key-down % todo m)})))))))