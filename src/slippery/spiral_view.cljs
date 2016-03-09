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

;;;;;;;; Game art ;;;;;;;;

(defn pad-click [event pad-index]
  (game/player-move common/Slippery pad-index)
  )

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
  ;; todo!
  (let [play-state (:play-state @(:game common/Slippery))
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

(defn show-game-size [view pads]
  (let [settings (:settings @(:game common/Slippery))
        game-size (:game-size settings)
        p (esg/xy->viewport view (pads game-size))]
    [:text.numb {:x (- (first p) 11.5)
            :y (+  (second p) 3)
            :font-family "FontAwesome"
            :font-size "30"
            :stroke "white"
            :stroke-width 1
            :fill "black"
            } "\uf00d"] ;x marks the spot
    ))

#_(defn show-numbers [view pads]
  (let [game @(:game common/Slippery)
        game-size (:game-size (:settings game))
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
                     (when (or (<= index state) (= index game-size)) [index point])) pads)))  )

(rum/defc viewer-macro < rum/reactive []
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
                             {:stroke "#3366bb"
                              :stroke-width 40
                              :stroke-dasharray "15 20  5 10"
                              :stroke-linecap "round"}
                             )

       ;; todo:
       #_(comp/render-pad-path view pad-count
                             0
                             (min game-size (+ (:limit settings)
                                     state))
                             {:stroke "#0088ff"
                              :stroke-width 30
                              :stroke-linecap "round"}
                             )


       ;; todo: render path so far
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
       ;; todo:
       (show-game-size view pads)

       ;; Current position of player
       (show-player view pads)

       ])]
   ])


(rum/defc help < rum/reactive []
  [:div {:style {:padding "20px"}}
   [:.alert.alert-info
    "On your turn you can build up to "
    [:b (:limit (:settings (rum/react (:game common/Slippery)))) " bridges"]
    " over the shallows by "
    [:b " tapping the yellow island you want to reach."]
    " Be the first to reach the treasure marked with a cross. "]])

(defrecord Spiral-view []
  IViewer
  (get-message [this status]
    (status messages))

  (get-fill [this status]
    ((status message-colours) colours))

  (game-viewer [this config] (viewer-macro))

  (help-viewer [this]
    (help))

  )
