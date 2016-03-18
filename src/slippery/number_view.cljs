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
           :vh 500
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

(def r 40)
(defn dy-dv []
  (/  (+ (:vh view) -10 (* -3 r)) (get-in @(:game common/Slippery) [:settings :game-size]))) ;(/ 30 6)
(defn dv-dy [] (/ 1 (dy-dv)))

(defn step-h [] (dy-dv))

(defn value->cy [value]
  (- 310 (* (dy-dv) value)))

(defn cy->value [cy]
  (- 310 (* (dv-dy) cy)))


(defn drag-started []
  (:drag-start @common/drag-state))

(defn handle-start-drag [event]
  (.preventDefault event)
  (.stopPropagation event)
  (when (game/player-can-move? common/Slippery)
    (let [target (.-target event)
          game @(:game common/Slippery)
          index (js/parseInt  (.getAttribute target "data-index"))
          state (:state (:play-state game))
          svg-coords (esg/mouse->svg (util/el "svg-container") common/svg-point event)]
      (swap! common/drag-state assoc :drag-start svg-coords :state state :index index))))

(defn handle-move [event]
  (.preventDefault event)
  (.stopPropagation event)
  (when (and (drag-started) (game/player-can-move? common/Slippery))
    (let [target (.-target event)
          index (js/parseInt (.getAttribute target "data-index"))
          svg-coords (esg/mouse->svg (util/el "svg-container") common/svg-point event)
          drag-start (:drag-start @common/drag-state)]
      (if drag-start
        (do
          (let [[dx dy] (map - svg-coords drag-start)
                game @(:game common/Slippery)
                state (:state (:play-state game))
                index (:index @common/drag-state)
                original ((:state @common/drag-state) index)
                left-value (if (zero? index) nil (get-in game [:play-state :state (dec index)]))
                dv (min (:limit (:settings game)) (* dy (dv-dy)))
                calc-value (max (- original dv) (inc left-value))
                ]
            (swap! (:game common/Slippery) assoc-in [:play-state :state index] (max 1 (min original  calc-value)))
            ))))))

(defn handle-end-drag [event]
  (.preventDefault event)
  (.stopPropagation event)
  (when (and (drag-started) (game/player-can-move? common/Slippery))
    (let [game @(:game common/Slippery)
          target (.-target event)
          index (:index @common/drag-state)
          original ((:state @common/drag-state) index)
          svg-coords (esg/mouse->svg (util/el "svg-container") common/svg-point event)
          diff (map - svg-coords (:drag-start @common/drag-state))]
      (when (not (js/isNaN index))
        (swap! (:game common/Slippery) update-in [:play-state :state index] Math.round))

      ;; have we made a valid move?
      (let [end-value (Math.round (nth (:state (:play-state game)) index))]
        (prn "end-value " end-value " original " original)
        (swap! common/drag-state assoc :drag-start nil)
        (when (< end-value original)
          (game/player-move common/Slippery [])))
      )))

(rum/defc dropper < rum/static [{:keys [:r :cx :cy] :as amap} value index svg]
  [:g.but {:style {:cursor "pointer"}
           :on-mouse-down handle-start-drag
           :on-mouse-move handle-move
           ;:on-mouse-out handle-end-drag
           :on-mouse-up handle-end-drag
           :on-touch-start handle-start-drag
           :on-touch-move handle-move
           :on-touch-end handle-end-drag
           }
   [:line {:line-width 1 :stroke "#000000" :stroke-width 2 :x1 (- cx 5) :x2 (- cx 5) :y1 cy :y2 (- cy (:vh view))}]
   [:line {:line-width 1 :stroke "#000000" :stroke-width 2 :x1 (+ cx 5) :x2 (+ cx 5) :y1 cy :y2 (- cy (:vh view))}]
   [:polygon (merge amap {:points (str cx ", " (+ cy (* 2 r)) " "
                                       (apply
                                        str
                                        (map
                                         #(str (+ cx (* r (js/Math.cos %))) ", "
                                               (+ cy (* r (js/Math.sin %))) " ")
                                         (range (/ Math.PI 6) (* -7 (/ Math.PI 6)) -0.15)))
                                       cx "," (+ cy (* 2 r))
                                       )
                          :data-value value
                          :data-index index})]

   [:text {:x (- (:cx amap) (if (< value 10) 17 33))
           :y (+ (:cy amap) 20)
           :fill (:text-fill amap)
           :font-size 60
           :key 2
           :style {:pointer-events "none"}
           } (js/Math.round value)]
   [:g {:transform "translate(0,0)"
        :style {:pointer-events "none"}}
    [:rect (merge amap {:fill "#ffffff" :width 20 :height (* (step-h) index) :x (- cx r -10) :y (+ cy (- (* 2 r) (- (step-h))))})]
    [:rect (merge amap {:fill "#ffffff" :width 20 :height (* (step-h) index) :x (+ -30 r cx) :y (+ cy (- (* 2 r) (- (step-h))))})]
    [:rect (merge amap {:fill "#ffffff" :width (+ 20 r r) :height (step-h) :x (- cx r 10) :y (+ cy (* 2 r))})]]
   [:use {:xlink-href "#hook2"
          :data-value value
          :data-index index
          :x (- cx 12)
          :y (+ cy (* 2 r) -15)}]
   ])

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
     [:div {:style {:margin-bottom "10px"}}]
     [:.row
      [:svg {:view-box (str "0 0 " (:vw view) " " (:vh view))
             :height "100%"
             :width "100%"
             :id "svg-container"
             :on-mouse-up handle-end-drag
             :on-touch-end handle-end-drag
             }
       [:defs
        [:path
         {:id "hook2" :fill "#000000" :stroke "none" ;:style {:pointer-events "none"}
          :d "M20.3,17.8c-1.2,0.7-2.3,2-4.3,3.3c-2,1.3-2.9,2.2-4.1,2.2c-2.3,0-3.8-1.1-3.8-3.7c0-2.6,3-5,4.9-6c0.3-0.1,0.5-0.3,0.8-0.5 c2.8-0.8,4.8-3.4,4.8-6.4c0-3.7-3-6.7-6.7-6.7c-1.2,0-2.3,0.3-3.3,0.9C6.2,2.1,3.5,5.5,1,12c-2.2,5.7-1.3,15.8,8.2,18.7 c8.1,2.4,13.3-3.9,15.1-9.7C25.1,18.3,21.5,17.1,20.3,17.8"}]]
       (map-indexed
        #(dropper {:cx (+ (:x origin) (* r (+ 1 (* 2 %1) (- (count state)))))
                   :cy (- (:vh view) (* 2 r) (* (dy-dv) %2) 10)
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

       [:rect {:fill   "white"
               :x      (+ r 10 (:x origin) (* r (+ 1 (* 2 (dec (count state))) (- (count state)))))
               :y      (- (:vh view) (+ (* (dy-dv) (count state)) 10))
               :height (+ (* (dy-dv) (count state)) 10)
               :width  400
               }]

       ]]]))


(rum/defc help < rum/reactive []
  [:div
   [:h3.center-block
    {:style {:color "white"
             :max-width "800px"}}
    "You win a silver dollar if you are the crane driver who lowers the last step of the staircase into place."
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
