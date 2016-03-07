(ns  ^:figwheel-always gotit.main
     (:require [rum.core :as rum]
               [generic.game :as game]
               [generic.util :as util]
               [generic.history :as hist]
               [generic.components :as comp]
               [generic.viewer :as iview :refer [IViewer]]
               [gotit.routing :as routing]
               [gotit.common :as common]
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
  (swap! (:game common/Gotit) assoc-in [:settings :players] count)
  (game/reset-game common/Gotit))

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
  (swap! (:game common/Gotit) #(assoc % :play-state (hist/undo! (:play-state %)))))

(defn redo
  "redo button handler"
  [event]
  (.preventDefault event)
  (swap! (:game common/Gotit) #(assoc % :play-state (hist/redo! (:play-state %)))))

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
  (handle-spinner event (:game common/Gotit) [:settings :target] #(validated-int (inc %) common/min-target common/max-target)))

(defn handle-dec-target [event]
  (handle-spinner event (:game common/Gotit) [:settings :target] #(validated-int (dec %) common/min-target common/max-target)))

(defn handle-inc-limit [event]
  (handle-spinner event (:game common/Gotit) [:settings :limit] #(validated-int (inc %) common/min-limit common/max-limit)))

(defn handle-dec-limit [event]
  (handle-spinner event (:game common/Gotit) [:settings :limit] #(validated-int (dec %) common/min-limit common/max-limit)))

(defn handle-int [event min-val max-val ref cursor]
  (.stopPropagation event)
  (.preventDefault event)
  (let [value (.parseInt js/window (.-value (.-target event)))]
    (swap! ref assoc-in cursor (validated-int value min-val max-val))
    ))

(defn new-pad-count [event]
  (handle-int event common/min-target common/max-target (:game common/Gotit) [:settings :target]))

(defn new-limit [event]
  (handle-int event common/min-limit common/max-limit (:game common/Gotit) [:settings :limit]))

(rum/defc settings-modal < rum/reactive []
  (let [active (fn [players player-count]
                 (if (= player-count players) "active" ""))
        game (rum/react (:game common/Gotit))
        stings (:settings game)]
    [:#settings.modal.fade {:tab-index "-1"
                            :role "dialog"
                            :aria-labelledby "mySmallModalLabel"}
     [:.modal-dialog.modal-sm
      [:.modal-content
       [:.modal-header
        [:button.close {:type "button"
                        :data-dismiss "modal"
                        :aria-label "Close"
                        }
         [:span.fa.fa-times {:aria-hidden "true"} ]]
        [:h4.modal-title "Settings"]]

       [:form.form-horizontal {:style {:padding "20px"}}
        [:form-group

         [:.row {:style {:padding "20px 0"}}

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

         [:.row {:style {:padding "20px 0"}}
          [:label.col-sm-4 {:for "p2"} "How many islands? "]
          [:span.spinner.col-sm-8
           [:button.up.no-select {:on-click handle-inc-target
                                  :on-touch-start handle-inc-target} "+"]
           [:button.down.no-select {:on-click handle-dec-target
                                    :on-touch-start handle-dec-target} "-"]
           [:input.num {:type "number"
                        :pattern "\\d*"
                        :input-mode "numeric"
                        :on-change new-pad-count
                        :value (:target (:settings game))}]]]
         [:.row {:style {:padding "20px 0"}}
          [:label.col-sm-4 {:for "p2"} "How many bridges per turn? "]
          [:span.spinner.col-sm-8
           [:button.up.no-select {:on-click handle-inc-limit
                                  :on-touch-start handle-inc-limit} "+"]
           [:button.down.no-select {:on-click handle-dec-limit
                                    :on-touch-start handle-dec-limit} "-"]
           [:input.num {:type "number"
                        :pattern "\\d*"
                        :input-mode "numeric"
                        :on-change new-limit
                        :value (:limit (:settings game))}]]]]]]]]))

(defn open-settings [event])

(defn close-settings [event])

(rum/defc tool-bar < rum/reactive []
  (let [active (fn [players player-count]
                 (if (= player-count players) "active" ""))
        game (rum/react (:game common/Gotit))
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
  [stings play]
  (let [[over-class status] (game/get-status common/Gotit)
        viewer (rum/react routing/Game-view)]
    [:div
      [:button {:type "button"
                :class "btn btn-danger"
                :style {:display "inline"
                        :clear "none"
                        }
                :on-click #(game/reset-game common/Gotit)
                :on-touch-end #(game/reset-game common/Gotit)}
       [:span {:class "fa fa-refresh"}]
       " Restart"]
     [:p {:class (str "status " over-class)
          :style {:width "240px"
                  :background-color (iview/get-fill viewer status)} :key "b4"} (iview/get-message viewer status)
]]))

(rum/defc footer < rum/reactive []
  "render footer with rules and copyright"

  [:section {:id "footer"}
   [:h2
    "The last player able to move wins"
    ]
   [:p
    "On your turn you may move the counter up to " (:limit (:settings (rum/react (:game common/Gotit)))) " squares"]
   ])

(rum/defc show-game-state < rum/reactive []
  (let [game (rum/react (:game common/Gotit))]
    [:.debug
     [:p (str (into {} (:settings game)))]
     [:p (str (into {} (:play-state game)))]
     [:p (str (rum/react hist/history))]]))

(rum/defc help < rum/reactive [debug?]
  [:div
   [:h3.center-block
    {:style {:color "white"
             :max-width "600px"}}
    "On your turn you can build up to "
    [:b (:limit (:settings (rum/react (:game common/Gotit)))) " bridges"]
    " over the shallows by "
    [:b " tapping the yellow island you want to reach."]
    " Be the first to reach the treasure marked with a cross. "
    (when debug? (show-game-state))]])

(rum/defc feedback < rum/reactive []
  (let [message (:feedback (:play-state (rum/react (:game common/Gotit))))]
    [:div {:style {:padding "0px 20px"
                   :position "relative"
                   :top "-50px"}}
     [:.alert {:style {:background-color "#437D9B"
                       :color "#ffffff"
                       :height "40px"
                       :display (if (= message "") "none" "block")}}
      message]]))

(rum/defc game-container  < rum/reactive
  "the game container mounted onto the html game element"
  []
  (let [game (rum/react (:game common/Gotit))
        play (:play-state game)]
    [:section.container-fluid {:id "game-container"
                         :style {:max-width "800px"}}
     (settings-modal)
     [:div.row ;{:class "full-width"}
      [:.col-sm-12
       [:p.center-block {:id "header"} (:title (:settings game))]
       (tool-bar play)
       (status-bar play)
       (help false)]
      (iview/game-viewer (rum/react routing/Game-view) play)
      (feedback)]
]))


;;;
;; game ui
;;;
(rum/defc main < rum/reactive []
  [:h1 (:title (:settings (rum/react (:game common/Gotit))))]
  )

(rum/mount (game-container) (util/el "main-app"))
