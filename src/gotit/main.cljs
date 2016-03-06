(ns  ^:figwheel-always gotit.main
     (:require [rum.core :as rum]
               [generic.game :as game]
               [generic.util :as util]
               [generic.history :as hist]
               [generic.components :as comp]
               [gotit.routing :as routing]
               [gotit.common :as common]
               [cljsjs.jquery :as jq]
               [cljsjs.bootstrap :as bs]
               [events.svg :as esg]
               ))

(enable-console-print!)

;;;
;; ui config
;;;
(def view {:vw 620
           :vh 620
           :pad-x 80
           :pad-y 80})

(def messages {:yours "Your turn"
               :als   "My turn"
               :as-turn "Blue's turn"
               :bs-turn "Red's turn"
               :you-win "You win!"
               :al-win "Computer wins!"
               :a-win "Blue won!"
               :b-win "Red won!"
               :draw  "It's a draw!"
               })

(def colours {:a "rgb(0, 153, 255)"
              :b "rgb(238, 68, 102)"
              :none "rgb(220,255,220)"
              :draw "rgb(74, 157, 97)"
              })

(def message-colours {:yours :b
                      :als   :a
                      :as-turn :a
                      :bs-turn :b
                      :you-win :b
                      :al-win :a
                      :a-win :a
                      :b-win :b
                      :draw :draw
                      })

(def computer-think-time 2000)

(defn get-message [status]
  (status messages))

(defn get-fill [status]
  ((status message-colours) colours))


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

#_(defn redo
  "redo button handler"
  [event]
  (.preventDefault event)
  (hist/redo!)
  (when-let [undo-peek (hist/peek-history!)]
    (swap! (:game common/Gotit) assoc :play-state undo-peek)))

;;;;;;;; Game art ;;;;;;;;

(defn pad-click [event pad-index]
  (game/player-move common/Gotit pad-index)
  )

(defn other-player [player]
    (if (= :a player) :b :a))

(defn reached?
  "look in history to discover whether a play-state has been reached"
  [history play-state]
  ((set (map #(dissoc % :feedback) (:undo history))) (dissoc play-state :feedback)))

(rum/defc pads-reached-by < rum/reactive [view pads player]
  [:g
   (map
    #(let [p (esg/xy->viewport view %)]
       ;; render flag
       [:text.numb {:x (- (first p) 15)
                    :y (+  (second p) 10)
                    :font-family "FontAwesome"
                    :font-size "30"
                    :fill (player colours)
                    } "\uf041"]) ; map-marker
    (keep-indexed (fn [index point]
                    (when (reached? (rum/react hist/history) (common/PlayState.
                                        player ""  index))
                      point))
                  pads))])

(defn show-player [view pads]
  (let [play-state (:play-state @(:game common/Gotit))
        p (esg/xy->viewport view (get pads (:state play-state)))]
    [:text.numb {:x (- (first p) 15)
            :y (+  (second p) 10)
            :font-family "FontAwesome"
            :font-size "30"
            :stroke "white"
            :stroke-width 1
            :fill ((:player play-state) colours)
            }  "\uf21d"] ;street-view
    ))

(defn show-target [view pads]
  (let [settings (:settings @(:game common/Gotit))
        target (:target settings)
        p (esg/xy->viewport view (pads target))]
    [:text.numb {:x (- (first p) 11.5)
            :y (+  (second p) 3)
            :font-family "FontAwesome"
            :font-size "30"
            :stroke "white"
            :stroke-width 1
            :fill "black"
            } "\uf00d"] ;x marks the spot
    ))

(defn show-numbers [view pads]
  (let [game @(:game common/Gotit)
        target (:target (:settings game))
        state (:state (:play-state game))]
    (map
     #(do
        (let [[dex p] %
              [left top] (esg/xy->viewport view p)]
          ;; render number
          [:text.numb {:x (+ 4 (if (< dex 10) (- left 6) (- left 13)))
                  :y (+ 10 (+ top 7))
                  :stroke "white"
                  :stroke-width 0.1
                  :font-size 18
                  :style {:font-weight 800}
                  :fill "black"
                  } dex]))
     (keep-indexed (fn [index point]
                     (when (or (<= index state) (= index target)) [index point])) pads)))  )

(rum/defc svg-container < rum/reactive []
  [:svg {:view-box (str "0 0 " (:vw view) " " (:vh view))
         :height "100%"
         :width "100%"
         :id "svg-container"
;;         :on-mouse-down esg/handle-start-line
;;         :on-mouse-move esg/handle-move-line
;;         :on-mouse-out esg/handle-out
;;         :on-mouse-up esg/handle-end-line
;;         :on-touch-start esg/handle-start-line
;;         :on-touch-move esg/handle-move-line
;;         :on-touch-end esg/handle-end-line
         }
   [:g {:transform "translate(-20, -20)"}
    (let [game (rum/react (:game common/Gotit))
          play-state (:play-state game)
          state (:state play-state)
          settings (:settings game)
          target (:target settings)
          limit (:limit settings)
          reached (:reached play-state)
          pad-count (inc target)
          pads (vec (comp/pad-spiral pad-count))]

      [:g
       ;; render sand banks
       (comp/render-pad-path view pad-count
                             0
                             target
                             {:stroke "#3366bb"
                              :stroke-width 40
                              :stroke-dasharray "15 20  5 10"
                              :stroke-linecap "round"}
                             )
       (comp/render-pad-path view pad-count
                             0
                             (min target (+ (:limit settings)
                                     state))
                             {:stroke "#0088ff"
                              :stroke-width 30
                              :stroke-linecap "round"}
                             )

       ;; render path so far
       (comp/render-pad-path view pad-count
                             0
                             state
                             {:stroke "#cc7700"
                              :stroke-width 20}
                             )

       ;; all islands
       (map-indexed #(comp/pad view %2 {:fill (cond
                                                (<= %1 state) "rgba(255, 160, 0, 0.8)"
                                                (< %1 (+ state limit 1)) "#ffcc00"
                                                :else "rgba(255, 160, 0, 0.6)")
                                        :stroke "none"
                                        :style {:pointer-events (if (and (> %1 state) (< %1 (+ state limit 1))) "auto" "none")}
                                        :n %1} (fn [event] (pad-click event %1))) pads)

       ;; Target Cross
       (show-target view pads)

       ;; islands reached by blue
       (pads-reached-by view pads :b)

       ;; islands reached by red
       (pads-reached-by view pads :a)

       ;; Current position of player
       (show-player view pads)

       ;; Number islands
       (show-numbers view pads)

       ])]
   ])


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
    [:div {:class "btn-group toolbar"}
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

(rum/defc status-bar
  "render top status bar"
  [stings play]
  (let [[over-class status] (game/get-status common/Gotit)]
    [:div {:style {:height "20px"}}
     [:p {:class (str "status " over-class)
          :style {:width "240px"
                  :background-color (get-fill status)} :key "b4"} (get-message status)
      [:button {:type "button"
                :class "btn btn-danger"
                :style {:display "inline"
                        :clear "none"
                        :float "right"
                        }
                :on-click #(game/reset-game common/Gotit)
                :on-touch-end #(game/reset-game common/Gotit)}
       [:span {:class "fa fa-refresh"}]]]]))

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
  [:div {:style {:padding "20px"}}
   [:.alert.alert-info
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
    [:section {:id "game-container"}
     (settings-modal)
     [:div {:class "full-width"}
      [:p {:id "header"} (:title (:settings game))]
      (tool-bar play)
      (status-bar play)]
     (help false)
     (svg-container play)
     (feedback)
     ;(footer)
]))


;;;
;; game ui
;;;
(rum/defc main < rum/reactive []
  [:h1 (:title (:settings (rum/react (:game common/Gotit))))]
  )

(rum/mount (game-container) (util/el "main-app"))
