(ns ^:figwheel-always gotit.rules
    (:require [sprague-grundy.core :as core]))
;;;
;; The state of gotit is completely defined by its settings
;;
;; :start - the number we start on
;; :target - the number we are aiming for
;; :limit - the maximum possible moves each turn
;;
;; and the current state.
;;;

(defn followers
  "followers are the states that can follow state according to the game rules."
  [target limit state]
  (if (set? state)
    (set (mapcat #(followers target limit  %) state))
    (set (map #(+ state %) (range 1 (inc (min (- target state) limit)))))))

(defn sample-followers
  "partially curried followers - returns a function of state"
  [sample]
  (partial followers (:target sample) (:limit sample)))


(defn heap-equivalent
  "The nim heap equivalent of a gotit state"
  [target limit state]
  (mod (- target state) (inc limit)))


(defn sample-heaps
  "partially curried heaps - returns a function of state"
  [sample]
  (partial heap-equivalent (:target sample) (:limit sample)))
