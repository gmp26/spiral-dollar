(ns  ^:figwheel-always gotit.main
     (:require [rum.core :as rum]
               [generic.util :as util]
               [generic.history :as hist]
               [generic.play :as play]
               [generic.components :as comp]
               [gotit.common :as common]
               [gotit.rules :as rules]
               [cljsjs.jquery :as jq]
               [cljsjs.bootstrap :as bs]
               [events.svg :as esg]
               ))


(.log js/console (util/el "main-app"))

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
               :you-win "Well done! You won"
               :al-win "Oops! You lost"
               :a-win "Blue won!"
               :b-win "Red won!"
               :draw  "It's a draw!"
               })

(def colours {:a "rgb(0, 153, 255)"
              :b "rgb(238, 68, 102)"
              :none "rgb(220,255,220)"
              :draw "rgb(74, 157, 97)"
              })

(def message-colours {:yours :a
                      :als   :b
                      :as-turn :a
                      :bs-turn :b
                      :you-win :a
                      :al-win :b
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
  (swap! common/settings update :players #(if (= % 1) 2 1))
  (common/reset-game))

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
  #_(undo!))

(defn redo
  "redo button handler"
  [event]
  (.preventDefault event)
  #_(redo!))

;;;;;;;; Game art ;;;;;;;;

(defn pad-click [event]
  (prn "you clicked"))


(defn pads-reached-by [view pads player color]
  (map
   #(do
      (let [p (esg/xy->viewport view %)]
        ;; render flag
        [:text {:x (- (first p) 15)
                :y (+  (second p) 10)
                :font-family "FontAwesome"
                :font-size "30"
                :fill (player colours)
                } "\uf024"])) ; flag
   (keep-indexed (fn [index point]
                   (when (= player (get (:reached @common/play-state) index)) point)) pads))  )

(defn show-player [view pads]
  (let [play-state @common/play-state
        p (esg/xy->viewport view (get pads (:state play-state)))]
    [:text {:x (- (first p) 15)
            :y (+  (second p) 10)
            :font-family "FontAwesome"
            :font-size "30"
            :stroke "white"
            :stroke-width 1
            :fill ((:player play-state) colours)
            }  "\uf21d"] ;street-view
    ))

(defn show-target [view pads]
  (let [settings @common/settings
        target (:target settings)
        p (esg/xy->viewport view (pads target))]
    [:text {:x (- (first p) 11.5)
            :y (+  (second p) 3)
            :font-family "FontAwesome"
            :font-size "30"
            :stroke "white"
            :stroke-width 1
            :fill "black"
            } "\uf00d"] ;x marks the spot
    ))

(defn show-numbers [view pads]
  (let [target (:target @common/settings)
        state (:state @common/play-state)]
    (map
     #(do
        (let [[dex p] %
              [left top] (esg/xy->viewport view p)]
          ;; render number
          [:text {:x (+ 4 (if (< dex 10) (- left 6) (- left 13)))
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
         :on-mouse-down esg/handle-start-line
         :on-mouse-move esg/handle-move-line
         :on-mouse-out esg/handle-out
         :on-mouse-up esg/handle-end-line
         :on-touch-start esg/handle-start-line
         :on-touch-move esg/handle-move-line
         :on-touch-end esg/handle-end-line
         }
   [:g {:transform "translate(-40, -20)"}
    (let [play-state (rum/react common/play-state)
          state (:state play-state)
          settings (rum/react common/settings)
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
                              :stroke-width 60
                              :stroke-dasharray "15 20  5 10"
                              :stroke-linecap "round"}
                             )
       (comp/render-pad-path view pad-count
                             0
                             (+ (:limit settings)
                                state)
                             {:stroke "#0088ff"
                              :stroke-width 40
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
       (map-indexed #(comp/pad view %2 {:fill "#ffaa00" :n %1} pad-click) pads)

       ;; islands reached by blue
       (pads-reached-by view pads :b "#0000ff")

       ;; islands reached by red
       (pads-reached-by view pads :a "#ff0000")

       ;; Current position of player
       (show-player view pads)

       ;; Target Cross
       (show-target view pads)

       ;; Number islands
       (show-numbers view pads)

       ])]
   ])

(rum/defc settings-modal < rum/reactive []
  (let [active (fn [players player-count]
                 (if (= player-count players) "active" ""))
        stings (rum/react common/settings)]
    [:#settings..modal.fade {:tab-index "-1"
                      :role "dialog"
                      :aria-labelledby "mySmallModalLabel"}
     [:.modal-dialog.modal-sm
      [:.modal-content
       [:.modal-header
        [:button.close {:type "button"
                        :data-dismiss "modal"
                        :aria-label "Close"
                        }
         [:span {:aria-hidden "true"} "x"]]
        [:h4.modal-title "Settings"]]
       [:button.btn.btn-default {:type "button" :class (active stings 1)
                                 :key "1"
                                 :on-click one-player
                                 :on-touch-start one-player}
        "1 player"]
       [:button {:type "button"
                 :class (str "btn btn-default " (active stings 2))
                 :key "2"
                 :on-click two-player
                 :on-touch-start two-player} "2 player"]]]])  )

(defn open-settings [event]
  (prn "open-modal called"))

(defn close-settings [event]
  (prn "close settings called"))

(rum/defc tool-bar < rum/reactive []
  (let [active (fn [players player-count]
                 (if (= player-count players) "active" ""))
        stings (rum/react common/settings)]
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


(defn get-status
  "derive win/lose/turn status"
  [stings play]
  (let [pa (= (:player stings) :a)
        gover (rules/game-over? stings play)
        over-class (if gover " pulsed" "")]
    (if (= (:players stings) 1)
      [over-class (cond
                    (= gover :a) :al-win
                    (= gover :b) :you-win
                    :else (if pa :yours :als))]
      [over-class (cond
                    (= gover :a) :b-win
                    (= gover :b) :a-win
                    :else (if pa :as-turn :bs-turn))])))


(rum/defc status-bar
  "render top status bar"
  [stings play]
  (let [[over-class status] (get-status stings play)]
    [:div
     [:p {:class (str "status " over-class)
          :style {:background-color (get-fill status)} :key "b4"} (get-message status)
      [:button {:type "button"
                :class "btn btn-danger"
                :style {:display "inline"
                        :clear "none"
                        :float "right"
                        }
                :on-click common/reset-game
                :on-touch-end common/reset-game}
       [:span {:class "fa fa-refresh"}]]]]))

(rum/defc footer < rum/reactive []
  "render footer with rules and copyright"

  [:section {:id "footer"}
   [:h2
    "The last player able to move wins"
    ]
   [:p
    "On your turn you may move the counter up to " (:limit (rum/react common/settings)) " squares"]
   ])

(rum/defc help < rum/reactive []
  [:div {:style {:padding "20px"}}
   [:.alert.alert-info
    "On your turn you can build up to "
    [:b (:limit (rum/react common/settings)) " bridges"]
    " over the shallows by "
    [:b " tapping the island you want to reach."]
    " Be the first to reach the treasure marked with a cross. "]])

(rum/defc game-container  < rum/reactive
  "the game container mounted onto the html game element"
  []
  (let [play (rum/react common/play-state)]
    [:section {:id "game-container"}
     (settings-modal)
     [:div {:class "full-width"}
      [:p {:id "header"} (:title (rum/react common/settings))]
      (tool-bar play)
      (status-bar play)]
     (help)
     (svg-container play)
     (footer)
     #_(debug-game play)]))


;;;
;; game ui
;;;
(rum/defc main < rum/reactive []
  [:h1 (:title (rum/react common/settings))]
  )

(rum/mount (game-container) (util/el "main-app"))
