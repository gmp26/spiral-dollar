(ns ^:figwheel-always generic.components
  (:require [rum.core :as rum]
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

(rum/defc pad [view [x y] & [click-handler]]
  "create a clickable game pad"
  (let [[left top] (esg/xy->viewport view [x y])]
    [:circle.pad {:r 20
                  :cx left
                  :cy top
                  :fill "red"
                  :stroke "#ffffff"
                  :stroke-width "2px"
                  :on-click click-handler
                  :on-touch-start click-handler
                  }]))

(rum/defc render-pad-path < rum/static [view pad-count from to & [styles]]
  "renders a spiral path from one pad to another"
  [:path (merge
          {:d (points->path view (pad-path pad-count from to))}
          {:fill "none"
           :stroke "#ff5533"
           :stroke-width "10px"}
          styles)])
