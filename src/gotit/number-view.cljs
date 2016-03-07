(ns ^:figwheel-always gotit.number-view
    (:require [rum.core :as rum]
              [generic.game :as game]
              [generic.util :as util]
              [generic.history :as hist]
              [generic.components :as comp]
              [generic.viewer :refer [IViewer]]
              [gotit.routing :as routing]
              [gotit.common :as common]
              [cljsjs.jquery :as jq]
              [cljsjs.bootstrap :as bs]
              [events.svg :as esg]))

;;;
;; ui config
;;;
(def view {:vw 620
           :vh 400
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
  (game/player-move common/Gotit pad-index)
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


(rum/defc number-in-circle < rum/static [amap value]
  (let [attrs (merge {:cx 0 :cy 0 :r 50 :fill "white" :text-fill "black" :stroke "black"} amap)]
    [:g
     [:circle attrs]
     [:text {:x (- (:cx attrs) (if (< value 10) 17 33))
             :y (+ (:cy attrs) 20)
             :fill (:text-fill attrs)
             :font-size 60
             }
      value]]))


(defn move-by [event delta]
  (.stopPropagation event)
  (.preventDefault event)
  (let [new-state (:state (:play-state @(:game common/Gotit)))])
   )


(rum/defc viewer-macro < rum/reactive []
  (let [game (rum/react (:game common/Gotit))
        play-state (:play-state game)
        state (:state play-state)
        settings (:settings game)
        target (:target settings)
        limit (:limit settings)
        origin {:x (/ (:vw view) 2) :y (/ (:vh view) 2)}]
    [:.col-sm-12
     [:.btn-group {:role "group" :aria-label "add to total buttons"}
      (for [delta (range 1 limit)]
        [:.btn.btn-default {:type "button"
                            :on-click #(move-by % delta)
                            :on-touch-start #(move-by % delta)} (str "+" delta)])]
     [:svg {:view-box (str "0 0 " (:vw view) " " (:vh view))
            :height "100%"
            :width "300px"
            :id "svg-container"
            ;;         :on-mouse-down esg/handle-start-line
            ;;         :on-mouse-move esg/handle-move-line
            ;;         :on-mouse-out esg/handle-out
            ;;         :on-mouse-up esg/handle-end-line
            ;;         :on-touch-start esg/handle-start-line
            ;;         :on-touch-move esg/handle-move-line
            ;;         :on-touch-end esg/handle-end-line
            }
      [:g {:transform "scale(1, 1) translate(-60 0)"}
       [:text {:x (- (:x origin) 170)
               :y (- (:y origin) 80)
               :fill "white"
               :font-size 40}
        "Total"]
       [:text {:x (- (:x origin) 170)
               :y (+ (:y origin) 20)
               :fill "white"
               :font-size 40}
        "Target"]
       (number-in-circle {:cx (:x origin)
                          :cy (- (:y origin) 100)
                          :r 40
                          :fill ((if (= (:player play-state) :a) :b :a) colours)
                          :stroke "none"
                          :text-fill "white"
                          } state)
       (number-in-circle {:cx (:x origin)
                          :cy (:y origin)
                          :r 40
                          :fill "#e90"
                          :stroke "none"
                          :text-fill "white"
                          } target)

       ]

      ]]))

(defrecord Number-view []
  IViewer
  (get-message [this status]
    (status messages))

  (get-fill [this status]
    ((status message-colours) colours))

  (game-viewer [this config] (viewer-macro))

  )
