(ns ^:figwheel-always slippery.number-view
    (:require [rum.core :as rum]
              [generic.game :as game]
              [generic.util :as util]
              [generic.history :as hist]
              [generic.components :as comp]
              [generic.viewer :refer [IViewer]]
              [slippery.common :as common]
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


(rum/defc number-in-circle < rum/static [amap value index]
  (let [attrs (merge {:cx 0 :cy 0 :r 50 :text-fill "black" :stroke "black"} amap)]
    [:g
     [:circle (conj attrs {:key 1 :data-value value :data-index index})]

]))

(def s30 0.5)
(def rt3 (Math.sqrt 3))
(def rt3o2 (/ rt3 2))

(defn value->cy [value]
  (- 310 (/ (* 20 value) 3)))


(defn handle-start-drag [event]
  (let [target (.-target event)
        svg-coords (esg/mouse->svg (util/el "svg-container") common/svg-point event)]
    (prn "value: " (js/parseInt  (.getAttribute target "data-value")))
    (prn "index" (js/parseInt (.getAttribute target "data-index")))
    (swap! common/drag-state assoc :drag-start svg-coords)))

(defn handle-move [event]
  (let [target (.-target event)
        index (js/parseInt (.getAttribute target "data-index"))
        svg-coords (esg/mouse->svg (util/el "svg-container") common/svg-point event)
        drag-start (:drag-start @common/drag-state)]
    (if drag-start
      (do
        (let [[dx dy] (map - svg-coords drag-start)
              actual (js/parseInt  (.getAttribute target "data-value"))
              calc-value (max 1 (min actual (js/Math.round (- actual (/ (* dy 3) 20)))))]
          (prn "actual-value" actual)
          (prn "calc-value" (max 1 (min actual (js/Math.round (- actual (/ (* dy 3) 20))))))
          (swap! (:game common/Slippery) update-in [:play-state :state index] calc-value)
          )))))

(defn handle-end-drag [event]
  (let [target (.-target event)
        svg-coords (esg/mouse->svg (util/el "svg-container") common/svg-point event)
        diff (map - svg-coords (:drag-start @common/drag-state))]
    (swap! common/drag-state assoc :drag-start nil)))


(rum/defc dropper < rum/static [{:keys [:r :cx :cy] :as amap} value index svg]
  [:g.but {:style {:cursor "pointer"}
           :on-mouse-down handle-start-drag
           :on-mouse-move handle-move
           :on-mouse-out handle-end-drag
           :on-mouse-up handle-end-drag
           :on-touch-start esg/handle-start-drag
           :on-touch-move esg/handle-move
           :on-touch-end esg/handle-end-drag
           }
   (number-in-circle amap value index)
   [:polygon (merge amap {:points (str (- cx (* rt3o2 r)) ", " (+ cy (* r s30)) " "
                                       (+ cx (* rt3o2 r)) ", " (+ cy (* r s30)) " "
                                       cx "," (+ cy (* 2 r)))
                          :data-value value
                          :data-index index})]
   [:text {:x (- (:cx amap) (if (< value 10) 17 33))
           :y (+ (:cy amap) 20)
           :fill (:text-fill amap)
           :font-size 60
           :key 2
           :style {:pointer-events "none"}
           } value]
   [:g {:transform "translate(-20,0)"
        :style {:pointer-events "none"}}
    [:rect (merge amap {:fill "#ffffff" :width 20 :height 6 :x (+ 10 (- cx r)) :y (+ cy (- (* 2 r) 6))})]
    [:rect (merge amap {:fill "#ffffff" :width 20 :height 6 :x (+ 10 cx) :y (+ cy (- (* 2 r) 6))})]
    [:rect (merge amap {:fill "#ffffff" :width 20 :height 6 :x (+ 10 (+ r cx)) :y (+ cy (- (* 2 r) 6))})]
    [:rect (merge amap {:fill "#ffffff" :width (* 3 r) :height 6 :x (- cx r) :y (+ cy (* 2 r))})]]
   ]

  )

(defn move-by [event delta]
  (.stopPropagation event)
  (.preventDefault event)
  (let [game @(:game common/Slippery)
        new-state (+ delta (:state (:play-state game)))]
    (when (<= new-state (:game-size (:settings game)))
      (game/player-move common/Slippery new-state))))

(rum/defc viewer-macro < rum/reactive []
  (let [game (rum/react (:game common/Slippery))
        play-state (:play-state game)
        state (:state play-state)
        settings (:settings game)
        game-size (:game-size settings)
        limit (:limit settings)
        origin {:x (/ (:vw view) 2) :y (/ (:vh view) 2)}
        r 40]
    [:.col-sm-12
     [:div {:style {:margin-bottom "50px"}}]


     [:.row
      [:svg {:view-box (str "0 0 " (:vw view) " " (:vh view))
             :height "100%"
             :width "100%"
             :id "svg-container"
             }

       (map-indexed
        #(dropper {:cx (+ (:x origin) (* r (+ 1 (* 2 %1) (- (count state)))))
                   :cy (+ (:y origin) (- (/ (* r %2) 6)) 110)
                   :r r
                   :fill ((if (= (:player play-state) :a) :b :a) colours)
                   :stroke "none"
                   :text-fill "white"
                   } %2 %1)
        state)

       [:rect {:fill "white"
              :x 0
              :y (- (:vh view) 10)
              :height 10
              :width "100%"}]

       ]]]))


(rum/defc help < rum/reactive []
  [:div
   [:h3.center-block
    {:style {:color "white"
             :max-width "800px"}}
    "On your turn you can pull down one of the numbers. The first player to make a neat staircase of bricks wins."
    ]])


(defrecord Number-view []
  IViewer
  (get-message [this status]
    (status messages))

  (get-fill [this status]
    ((status message-colours) colours))

  (game-viewer [this config] (viewer-macro))

  (help-viewer [this]
    (help))

  )
