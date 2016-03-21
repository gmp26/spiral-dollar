(ns ^:figwheel-always slippery.spiral-view
  (:require [rum.core :as rum]
            [generic.game :as game]
            [generic.util :as util]
            [generic.history :as hist]
            [generic.components :as comp]
            [generic.viewer :refer [IViewer]]
            [slippery.common :as common]
            [cljsjs.jquery :as jq]
            [cljsjs.bootstrap :as bs]
            [events.svg :as esg]))

;;;
;; ui config
;;;
(def view {:vw    620
           :vh    620
           :pad-x 80
           :pad-y 80})

(def messages {:yours   "Your turn"
               :als     "My turn"
               :as-turn "Blue's turn"
               :bs-turn "Red's turn"
               :you-win "You win!"
               :al-win  "Computer wins!"
               :a-win   "Blue won!"
               :b-win   "Red won!"
               :draw    "It's a draw!"
               })

(def colours {:a    "rgb(0, 153, 255)"
              :b    "rgb(238, 68, 102)"
              :none "rgb(220,255,220)"
              :draw "rgb(74, 157, 97)"
              })

(def message-colours {:yours   :b
                      :als     :a
                      :as-turn :a
                      :bs-turn :b
                      :you-win :b
                      :al-win  :a
                      :a-win   :a
                      :b-win   :b
                      :draw    :draw
                      })

(def computer-think-time 2000)

;;;;;;;; Game art ;;;;;;;;

(defn pad-click [event pad-index]
  (prn "pad " pad-index " clicked")
  (game/player-move common/Slippery pad-index)
  )

(defn reached?
  "look in history to discover whether a play-state has been reached"
  [history play-state]
  ((set (map #(dissoc % :feedback) (:undo history))) (dissoc play-state :feedback)))

(rum/defc
  pads-reached-by < rum/reactive [view pads player]
  [:g
   (map
     #(let [p (esg/xy->viewport view %)]
       ;; render flag
       [:text.numb {:x           (- (first p) 15)
                    :y           (+ (second p) 10)
                    :font-family "FontAwesome"
                    :font-size   "30"
                    :fill        (player colours)
                    } "\uf041"])                            ; map-marker
     (keep-indexed (fn [index point]
                     (when (reached? (rum/react hist/history) (common/PlayState.
                                                                player "" index))
                       point))
                   pads))])

(defn show-players
  "show player positions on spiral"
  [view pads]
  (let [play-state (:play-state @(:game common/Slippery))
        p-locs (map #(esg/xy->viewport view %) (map #(get pads %) (:state play-state)))]
    (map-indexed
      (fn [index p] [:text.numb {:x            (- (first p) 11)
                                 :y            (+ (second p) 14)
                                 :font-family  "FontAwesome"
                                 :font-size    "38"
                                 :stroke       "white"
                                 :stroke-width 2
                                 :fill         "black"
                                 :key          index
                                 :style {:pointer-events "none"}
                                 } "\uf188"])
      p-locs)
    ))

(defn show-game-size [view pads]
  (let [settings (:settings @(:game common/Slippery))
        game-size (:game-size settings)
        p (esg/xy->viewport view (pads game-size))]
    [:text.numb {:x            (- (first p) 11.5)
                 :y            (+ (second p) 10)
                 :font-family  "FontAwesome"
                 :font-size    "30"
                 :stroke       "white"
                 :stroke-width 2
                 :fill         "black"
                 :style {:pointer-events "none"}
                 } "\uf005"]                                ;x marks the spot
    ))

(rum/defc
  viewer-macro < rum/reactive []
  [:svg {:view-box (str "0 0 " (:vw view) " " (:vh view))
         :height   "100%"
         :width    "100%"
         :id       "svg-container"
         ;;         :on-mouse-down esg/handle-start-line
         ;;         :on-mouse-move esg/handle-move-line
         ;;         :on-mouse-out esg/handle-out
         ;;         :on-mouse-up esg/handle-end-line
         ;;         :on-touch-start esg/handle-start-line
         ;;         :on-touch-move esg/handle-move-line
         ;;         :on-touch-end esg/handle-end-line
         }
   [:g {:transform "translate(-20, -20)"}
    (let [game (rum/react (:game common/Slippery))
          play-state (:play-state game)
          state (:state play-state)
          settings (:settings game)
          game-size (:game-size settings)
          limit (:limit settings)
          pad-count (inc game-size)
          pads (vec (comp/pad-spiral pad-count))]

      [:g
       ;; render sand banks
       (comp/render-pad-path view pad-count
                             0
                             game-size
                             {:stroke         "#b06000"
                              :stroke-width   (- 174 (* pad-count (/ 125 40)))
                              :stroke-linecap "round"
                              }
                             )

       (comp/render-pad-path view pad-count
                             0
                             game-size
                             {:stroke           "rgba(220,140,0,1)"
                              :stroke-width     (- 150 (* pad-count (/ 125 45)))
                              :stroke-dasharray "15 20"
                              ;:stroke-linecap "round"
                              }
                             )

       ;; all islands
       (map-indexed #(comp/pad view %2 {:fill   (cond
                                                  (and (> (count state) 0) (not ((set state) %1)) (> %1 (state 0))) "rgba(0, 0, 0, 0.4)"
                                                  (< %1 (+ state limit 1)) "#ffcc00"
                                                  :else "rgba(0, 0, 0, 0)")
                                        :stroke "none"
                                        :style  {:pointer-events (if (and (not ((set state) %1)) (> %1 (state 0))) "auto" "none")}
                                        :n      %1} (fn [event] (pad-click event %1))) pads)

       ;; Target Cross
       ;; todo:
       (show-game-size view pads)

       ;; Current position of players
       (show-players view pads)

       ])]
   ])


(rum/defc
  help < rum/reactive []
  [:div
   [:h3.center-block
    {:style {:color     "white"
             :max-width "800px"}}
    "Tap a spot to move a bug towards the snail's starry mouth. The winner moves last."]])

((defrecord Spiral-view []
   IViewer
   (get-message [this status]
     (status messages))

   (get-fill [this status]
     ((status message-colours) colours))

   (game-viewer [this config] (viewer-macro))

   (help-viewer [this]
     (help))

   ))
