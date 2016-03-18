(ns  ^:figwheel-always slippery.main
     (:require [rum.core :as rum]
               [generic.game :as game]
               [generic.util :as util]
               [generic.history :as hist]
               [generic.viewer :as iview :refer [IViewer]]
               [slippery.routing :as routing]
               [slippery.common :as common]
               [slippery.spiral-view :refer [Spiral-view]]
               [slippery.number-view :refer [Number-view]]
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
  (.stopPropagation event)
  (change-player-count 1))

(defn two-player [event]
  (.preventDefault event)
  (.stopPropagation event)
  (change-player-count 2))

(defn limited [event]
  (.preventDefault event)
  (.stopPropagation event)
  (swap! (:game common/Slippery) assoc-in [:settings :limit] 20))

(defn unlimited [event]
  (.preventDefault event)
  (.stopPropagation event)
  (swap! (:game common/Slippery) assoc-in [:settings :limit] 1000)
  )

(defn computer-first [event]
  (.preventDefault event)
  (.stopPropagation event)
  (swap! (:game common/Slippery) assoc-in [:play-state :player] :b))

(defn you-first [event]
  (.preventDefault event)
  (.stopPropagation event)
  (swap! (:game common/Slippery) assoc-in [:play-state :player] :a)
  )

(defn undo
  "undo button handler"
  [event]
  (.preventDefault event)
  (.stopPropagation event)
  (swap! (:game common/Slippery) #(assoc % :play-state (hist/undo! (:play-state %)))))

(defn redo
  "redo button handler"
  [event]
  (.preventDefault event)
  (.stopPropagation event)
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

(defn handle-inc-game-size [event]
  (handle-spinner event (:game common/Slippery) [:settings :game-size] #(validated-int (inc %) common/min-game-size common/max-game-size)))

(defn handle-dec-game-size [event]
  (handle-spinner event (:game common/Slippery) [:settings :game-size] #(validated-int (dec %) common/min-game-size common/max-game-size)))

(defn handle-inc-coin-count [event]
  (handle-spinner event (:game common/Slippery) [:settings :coin-count] #(validated-int (inc %) common/min-coin-count common/max-coin-count)))

(defn handle-dec-coin-count [event]
  (handle-spinner event (:game common/Slippery) [:settings :coin-count] #(validated-int (dec %) common/min-coin-count common/max-coin-count)))

(defn handle-inc-limit [event]
  (handle-spinner event (:game common/Slippery) [:settings :limit] #(validated-int (inc %) common/min-limit common/max-limit)))

(defn handle-dec-limit [event]
  (handle-spinner event (:game common/Slippery) [:settings :limit] #(validated-int (dec %) common/min-limit common/max-limit)))

(defn handle-int [event min-val max-val ref cursor]
  (.stopPropagation event)
  (.preventDefault event)
  (let [value (.parseInt js/window (.-value (.-game-size event)))]
    (swap! ref assoc-in cursor (validated-int value min-val max-val))
    ))

(defn new-pad-count [event]
  (handle-int event common/min-game-size common/max-game-size (:game common/Slippery) [:settings :game-size]))

(defn new-coin-count [event]
  (handle-int event common/min-coin-count common/max-coin-count (:game common/Slippery) [:settings :coin-count]))

(defn new-limit [event]
  (handle-int event common/min-limit common/max-limit (:game common/Slippery) [:settings :limit]))

(defn switch-view [viewer]
  (common/switch-view viewer)
  (routing/save-settings))

(defn hidden-settings
  [_]
  (routing/save-settings))

(defn open-settings
  "add modal close detection"
  [_]
  (.on (js/$ "#settings") "hidden.bs.modal" hidden-settings))

(defn close-settings
  "remove eventhandler to avoid memory leak"
  [event]
  (routing/save-settings)
  (.off (js/$ "#settings") "hidden.bs.modal")
  (game/reset-game common/Slippery)
  (when (game/is-computer-turn? common/Slippery)
    (game/schedule-computer-turn common/Slippery)))


(rum/defc selector < rum/static [select-1? label1 label2 action1 action2]
          [:div
           [:button.btn.btn-default.dropdown-toggle
            {:type          "button"
             :data-toggle   "dropdown"
             :aria-haspopup "true"
             :aria-expanded "false"}
            (if (select-1?) label1 label2)
            [:span.caret]]
           [:ul.dropdown-menu
            [:li [:a {:href "#" :on-click action1} label1]]
            [:li [:a {:href "#" :on-click action2} label2]]]])

(rum/defc spinner < rum/static [value on-change on-up on-down]
  [:div
   [:span.spinner.col-sm-7
    [:button.up.no-select {:on-click on-up
                           :on-touch-start on-up} "+"]
    [:button.down.no-select {:on-click on-down
                             :on-touch-start on-down} "-"]
    [:input.num {:type "number"
                 :pattern "\\d*"
                 :input-mode "numeric"
                 :on-change on-change
                 :value value}]]])


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

          [:label.col-sm-4 "Choose game"]
          [:.col-sm-8
           (selector #(= :number (:viewer (:settings game)))
                     "Silver Dollar" "Slippery Snail"
                     #(switch-view :number)
                     #(switch-view :island))]]

         [:.row {:style {:padding "10px 0"}}
          [:label.col-sm-4 "Game mode"]
          [:.col-sm-8
           (selector #(= 1 (:players (:settings game)))
                     "Play the computer " "Play an opponent "
                     one-player two-player)]]

         (when (= 1 (:players (:settings game)))
           [:.row {:style {:padding "10px 0"}}
            [:label.col-sm-4 "First player:"]
            [:.col-sm-8
             (selector #(= :b (:player (:play-state game)))
                       "The computer " "You "
                       computer-first you-first)]])

         [:.row {:style {:padding "10px 0"}}
          [:label.col-sm-5 {:for "p2"} "Game length"]
          (spinner (:game-size (:settings game)) new-pad-count handle-inc-game-size handle-dec-game-size)]

         [:.row {:style {:padding "10px 0"}}
          [:label.col-sm-5 {:for "p2"} "Game count"]
          (spinner (:coin-count (:settings game)) new-coin-count handle-inc-coin-count handle-dec-coin-count)]

         [:.row {:style {:padding "10px 0"}}
          [:label.col-sm-4 {:for "p3"} "Moves are "]
          [:.col-sm-8
           (selector #(= 1000 (:limit (:settings game)))
                     "Unlimited " "Limited "
                     unlimited limited)]]


         (when (not= 1000 (:limit (:settings game)))
           [:.row {:style {:padding "10px 0"}}
            [:label.col-sm-5 {:for "p2"} (if (= :number (:viewer (:settings game)))
                                           "Move no more than")]
            (spinner (:limit (:settings game)) new-limit handle-inc-limit handle-dec-limit)])


         ]]]]]))

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
       " New game"]
     [:p {:class (str "status " over-class)
          :style {:width "100%"
                  :background-color (iview/get-fill viewer status)}
          :key 2} (iview/get-message viewer status)]]))

(rum/defc footer < rum/reactive []


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
     [:p (str (rum/react hist/history))]
     [:p (str (rum/react common/drag-state))]]))


(rum/defc feedback < rum/reactive []
  (let [message (:feedback (:play-state (rum/react (:game common/Slippery))))]
    [:div {:style {:padding "0px 20px"
                   :position "relative"
                   :top "-250px"}}
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
     [:div.row                                              ;{:class "full-width"}
      [:.col-sm-12
       [:p.center-block {:id "header"} (:title (:settings game))]
       (tool-bar play)
       (status-bar viewer)
       (iview/help-viewer viewer)
       ;(show-game-state)
       ]
      (iview/game-viewer viewer play)
      #_(feedback)
      ;(show-game-state)
      ]]))


;;;
;; game ui
;;;
(rum/defc main < rum/reactive []
  [:h1 (:title (:settings (rum/react (:game common/Slippery))))]
  )

(rum/mount (game-container) (util/el "main-app"))
