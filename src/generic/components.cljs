(ns ^:figwheel-always generic.components
    (:require [rum.core :as rum]
              [gotit.common :as common]
              [events.svg :as esg]
))

(defn degrees-of-turn [pad-count]
  "return the degrees a spiral should turn through in order to place pad-count pads"
  (* 40 pad-count))

(defn pad-spiral [pad-count]
  "returns the centres of equally spaced pads placed on a spiral"
  (let [lambda (/ 1 2 Math.PI)
        deg-of-turn (degrees-of-turn pad-count)
        step (/ deg-of-turn pad-count)
        step-size (/ 1.00001 pad-count)
        turns (/ deg-of-turn 360)]
    (for [mu  (range 0 1 step-size)
          :let [deg (* (Math.sqrt  mu) deg-of-turn)
                theta (/ (* deg Math.PI) 180)]]
      [(/ (* lambda theta (Math.sin theta)) turns)
       (- (/ (* lambda theta (Math.cos theta)) turns))
       ]))
  )

(defn pad-path [pad-count from to]
  "returns points on a spiral path from one pad to another"
  (let [lambda (/ 1 2 Math.PI)
        deg-of-turn (degrees-of-turn pad-count)
        step (/ deg-of-turn pad-count)
        step-size (/ 1.00001 pad-count)
        turns (/ deg-of-turn 360)]
    (for [mu  (range (* from step-size) (* to step-size) (/ step-size 20))
          :let [deg (* (Math.sqrt  mu) deg-of-turn)
                theta (/ (* deg Math.PI) 180)]]
      [(/ (* lambda theta (Math.sin theta)) turns)
       (- (/ (* lambda theta (Math.cos theta)) turns))
       ])) )

(defn points->path [view points]
  "return an svg path by joining the dots supplied in points"
  (let [origin (esg/xy->viewport view (first points))]
    (str "M" (origin 0) " " (origin 1) " "
         (apply str (map #(str "L" (% 0) " " (% 1) " ")
                         (map #(esg/xy->viewport view %) (rest points)))))))

;; Actually, now a gotit specific pad.  Mmmm
(rum/defc pad < rum/reactive [view [x y] & [attributes handler]]
  "create a clickable game pad"
  (let [[left top] (esg/xy->viewport view [x y])
        attrs (conj {} attributes (if handler {:on-click handler :on-touch-start handler} {}))
        target (:target (rum/react common/settings))
        state (:state (rum/react common/play-state))
        n (:n attributes)
        ]
    [:g
     [:circle.pad (merge {:r 20
                          :cx left
                          :cy top
                          :fill "red"
                          :stroke "#ffffff"
                          :stroke-width "2px"
                          } attrs)]
     [:text {:x (+ 25 (if (< n 10) (- left 6) (- left 13)))
             :y (+ -25 (+ top 7))
               :font-family "Verdana"
               :font-size "20"
               :fill "white"
             } (str n)]

     ;; street-view marks me
     (when (= n state)
       [:text {:x (- left 11.5)
               :y (+ top 9.5)
               :font-family "FontAwesome"
               :font-size "30"
               :fill "white"
               } "\uf21d"]
       )

     ;; x marks the spot
     (when (= n target)
       [:text {:x (- left 11.5)
               :y (+ top 9.5)
               :font-family "FontAwesome"
               :font-size "30"
               :fill "white"
               } "\uf00d"]
       )]))

(rum/defc render-pad-path < rum/static [view pad-count from to & [styles]]
  "renders a spiral path from one pad to another"
  [:path (merge
          {:d (points->path view (pad-path pad-count from to))}
          {:fill "none"
           :stroke "#ff5533"
           :stroke-width "10px"}
          styles)])
