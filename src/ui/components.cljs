(ns ui.components
  (:require [rum.core :as rum]
            [events.svg :as esg]
))

(defn pad-spiral [deg-of-turn pad-count]
  (let [lambda (/ 1 2 Math.PI)
        step (/ deg-of-turn pad-count)
        step-size (/ 1 pad-count)
        turns (/ deg-of-turn 360)]
    (for [mu  (range 0 1 step-size)
          :let [deg (* (Math.sqrt  mu) deg-of-turn)
                theta (/ (* deg Math.PI) 180)]]
      [(/ (* lambda theta (Math.sin theta)) turns)
       (- (/ (* lambda theta (Math.cos theta)) turns))
       ]))
  )

(defn pad-path [deg-of-turn pad-count from to]
  (let [lambda (/ 1 2 Math.PI)
        step (/ deg-of-turn pad-count)
        step-size (/ 1 pad-count)
        turns (/ deg-of-turn 360)]
    (for [mu  (range (* from step-size) (* to step-size) (/ step-size 50))
          :let [deg (* (Math.sqrt  mu) deg-of-turn)
                theta (/ (* deg Math.PI) 180)]]
      [(/ (* lambda theta (Math.sin theta)) turns)
       (- (/ (* lambda theta (Math.cos theta)) turns))
       ])) )

(defn points->path [view points]
  (let [origin (esg/xy->viewport view (first points))]
    (str "M" (origin 0) " " (origin 1) " "
         (apply str (map #(str "L" (% 0) " " (% 1) " ")
                         (map #(esg/xy->viewport view %) (rest points)))))))

(rum/defc pad [view [x y] & [click-handler]]
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

(rum/defc render-pad-path < rum/static [view deg-of-turn pad-count from to & [styles]]

  [:path (merge
          {:d (points->path view (pad-path deg-of-turn pad-count from to))}
          {:fill "none"
           :stroke "#ff5533"
           :stroke-width "10px"}
          styles)])
