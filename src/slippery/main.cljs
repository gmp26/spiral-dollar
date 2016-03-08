(ns  ^:figwheel-always slippery.main
     (:require [rum.core :as rum]
               [generic.game :as game]
               [generic.util :as util]
               [generic.history :as hist]
               [generic.components :as comp]
               [generic.viewer :as iview :refer [IViewer]]
               [slippery.routing :as routing]
               [slippery.common :as common]
               [slippery.spiral-view :refer [Spiral-view]]
               [slippery.number-view :refer [Number-view]]
               [cljsjs.jquery :as jq]
               [cljsjs.bootstrap :as bs]
               [events.svg :as esg]
               ))

(enable-console-print!)


;;;
;; ui button events
;;;

(defn change-player-count
  "change to 1-player or 2-player mode"
  [count]
  (swap! (:game common/Slippery) assoc-in [:settings :players] count)
  (game/reset-game common/Slippery))

(defn one-player [event]
  (.preventDefault event)
  (change-player-count 1))

(defn two-player [event]
  (.preventDefault event)
  (change-player-count 2))

(defn undo
  "undo button handler"
  [event]
  (.preventDefault event)
  (swap! (:game common/Slippery) #(assoc % :play-state (hist/undo! (:play-state %)))))

(defn redo
  "redo button handler"
  [event]
  (.preventDefault event)
  (swap! (:game common/Slippery) #(assoc % :play-state (hist/redo! (:play-state %)))))

(defn validated-int [value min-val max-val]
  (cond
    (< value min-val) min-val
    (> value max-val) max-val
    :else value))

(defn handle-spinner
  "spinner clicked, change @ref.cursor using op"
  [event ref cursor op]
  (swap! ref update-in cursor op)
  (.preventDefault event)
  (.stopPropagation event))

(defn handle-inc-target [event]
  (handle-spinner event (:game common/Slippery) [:settings :target] #(validated-int (inc %) common/min-target common/max-target)))

(defn handle-dec-target [event]
  (handle-spinner event (:game common/Slippery) [:settings :target] #(validated-int (dec %) common/min-target common/max-target)))

(defn handle-inc-limit [event]
  (handle-spinner event (:game common/Slippery) [:settings :limit] #(validated-int (inc %) common/min-limit common/max-limit)))

(defn handle-dec-limit [event]
  (handle-spinner event (:game common/Slippery) [:settings :limit] #(validated-int (dec %) common/min-limit common/max-limit)))

(defn handle-int [event min-val max-val ref cursor]
  (.stopPropagation event)
  (.preventDefault event)
  (let [value (.parseInt js/window (.-value (.-target event)))]
    (swap! ref assoc-in cursor (validated-int value min-val max-val))
    ))

(defn new-pad-count [event]
  (handle-int event common/min-target common/max-target (:game common/Slippery) [:settings :target]))

(defn new-limit [event]
  (handle-int event common/min-limit common/max-limit (:game common/Slippery) [:settings :limit]))

(defn switch-view [viewer]
  (common/switch-view viewer)
  (routing/save-settings))

(defn hidden-settings [event]
  (routing/save-settings))

(defn open-settings
  "add modal close detection"
  [event]
  (.on (js/$ "#settings") "hidden.bs.modal" hidden-settings))

(defn close-settings
  "remove eventhandler to avoid memory leak"
  [event]
  (routing/save-settings)
  (.off (js/$ "#settings") "hidden.bs.modal"))

(rum/defc settings-modal < rum/reactive []
  (let [active (fn [players player-count]
                 (if (= player-count players) "active" ""))
        game (rum/react (:game common/Slippery))
        stings (:settings game)]
    [:#settings.modal.fade {:tab-index "-1"
                            :role "dialog"
                            :aria-labelledby "mySmallModalLabel"
                            }
     [:.modal-dialog.modal-sm
      [:.modal-content
       [:.modal-header
        [:button.close {:type "button"
                        :data-dismiss "modal"
                        :aria-label "Close"
                        :on-click close-settings
                        }
         [:span.fa.fa-times {:aria-hidden "true"} ]]
        [:h4.modal-title "Settings"]]

       [:form.form-horizontal {:style {:padding "20px"}}
        [:form-group

         [:.row {:style {:padding "10px 0"}}

          [:label.col-sm-4 {:for "p1"} "Choose game"]
          [:.btn-group.col-sm-8
           [:button.btn.btn-default.dropdown-toggle
            {:type "button"
             :data-toggle "dropdown"
             :aria-haspopup "true"
             :aria-expanded "false"}
            (if (= :number (:viewer (:settings game))) "Classic Got it " "Got it Island ")
            [:span.caret]]
           [:ul.dropdown-menu
            [:li [:a {:href "#" :on-click #(switch-view :number)} "Classic Got it"]]
            [:li [:a {:href "#" :on-click #(switch-view :island)} "Got it Island"]]]]][:.row {:style {:padding "20px 0"}}

          [:label.col-sm-4 {:for "p1"} "Game mode"]
          [:.btn-group.col-sm-8
           [:button.btn.btn-default.dropdown-toggle
            {:type "button"
             :data-toggle "dropdown"
             :aria-haspopup "true"
             :aria-expanded "false"}
            (if (= 1 (:players (:settings game))) "Play the computer " "Play an opponent ")
            [:span.caret]]
           [:ul.dropdown-menu
            [:li [:a {:href "#" :on-click one-player} "Play the computer"]]
            [:li [:a {:href "#" :on-click two-player} "Play an opponent"]]]]]

         [:.row {:style {:padding "10px 0"}}
          [:label.col-sm-5 {:for "p2"} (if (= :number (:viewer (:settings game)))
                                         "Target number" "How many islands?")]
          [:span.spinner.col-sm-7
           [:button.up.no-select {:on-click handle-inc-target
                                  :on-touch-start handle-inc-target} "+"]
           [:button.down.no-select {:on-click handle-dec-target
                                    :on-touch-start handle-dec-target} "-"]
           [:input.num {:type "number"
                        :pattern "\\d*"
                        :input-mode "numeric"
                        :on-change new-pad-count
                        :value (:target (:settings game))}]]]
         [:.row {:style {:padding "10px 0"}}
          [:label.col-sm-5 {:for "p2"} (if (= :number (:viewer (:settings game)))
                                         "Each turn, add no more than" "Each turn, bridge no more than")]
          [:span.spinner.col-sm-7
           [:button.up.no-select {:on-click handle-inc-limit
                                  :on-touch-start handle-inc-limit} "+"]
           [:button.down.no-select {:on-click handle-dec-limit
                                    :on-touch-start handle-dec-limit} "-"]
           [:input.num {:type "number"
                        :pattern "\\d*"
                        :input-mode "numeric"
                        :on-change new-limit
                        :value (:limit (:settings game))}]
           ]]]]]]]))

(rum/defc tool-bar < rum/reactive []
  (let [active (fn [players player-count]
                 (if (= player-count players) "active" ""))
        game (rum/react (:game common/Slippery))
        stings (:settings game)]
    [:.btn-group.toolbar.pull-right
     [:button.btn.btn-default.bs-example-modal-sm
      {:type "button"
       :key 1
       :data-target "#settings"
       :data-toggle "modal"
       :on-click open-settings
       :on-touch-start open-settings}
      "Settings"]


     [:button {:type "button"
               :class "btn btn-info"
               :key "bu5"
               :on-click undo
               :on-touch-start undo}
      [:span {:class "fa fa-undo"}]]
     [:button {:type "button"
               :class "btn btn-info"
               :key "bu6"
               :on-click redo
               :on-touch-start redo}
      [:span {:class "fa fa-repeat"}]]]
     ))

(rum/defc status-bar < rum/reactive
  "render top status bar"
  [viewer]
  (let [[over-class status] (game/get-status common/Slippery)]
    [:div
      [:button {:type "button"
                :class "btn btn-danger"
                :style {:display "inline"
                        :clear "none"
                        }
                :on-click #(game/reset-game common/Slippery)
                :on-touch-end #(game/reset-game common/Slippery)
                :key 1}
       [:span {:class "fa fa-refresh"}]
       " Restart"]
     [:p {:class (str "status " over-class)
          :style {:width "100%"
                  :background-color (iview/get-fill viewer status)}
          :key 2} (iview/get-message viewer status)]]))

(rum/defc footer < rum/reactive []
  "render footer with rules and copyright"

  [:section {:id "footer"}
   [:h2
    "The last player able to move wins"
    ]
   [:p
    "On your turn you may move the counter up to " (:limit (:settings (rum/react (:game common/Slippery)))) " squares"]
   ])

(rum/defc show-game-state < rum/reactive []
  (let [game (rum/react (:game common/Slippery))]
    [:.debug
     [:p (str (into {} (:settings game)))]
     [:p (str (into {} (:play-state game)))]
     [:p (str (rum/react hist/history))]]))


(rum/defc feedback < rum/reactive []
  (let [message (:feedback (:play-state (rum/react (:game common/Slippery))))]
    [:div {:style {:padding "0px 20px"
                   :position "relative"
                   :top "-50px"}}
     [:p {:style {:color "#ffffff"
                  :height "40px"
                  :font-size "18px"
                  :display (if (= message "") "none" "block")}}
      message]]))

(rum/defc game-container  < rum/reactive
  "the game container mounted onto the html game element"
  []
  (let [game (rum/react (:game common/Slippery))
        viewer (if (= :number (:viewer (:settings game))) (Number-view.) (Spiral-view.))
        play (:play-state game)]
    [:section#game-container.container {:style {:max-width "600px"}}
     (settings-modal)
     [:div.row ;{:class "full-width"}
      [:.col-sm-12
       [:p.center-block {:id "header"} (:title (:settings game))]
       (tool-bar play)
       (status-bar viewer)
       (iview/help-viewer viewer)
       ;(show-game-state)
       ]
      (iview/game-viewer viewer play)
      (feedback)]
]))


;;;
;; game ui
;;;
(rum/defc main < rum/reactive []
  [:h1 (:title (:settings (rum/react (:game common/Slippery))))]
  )

(rum/mount (game-container) (util/el "main-app"))
