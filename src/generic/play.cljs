(ns ^:figwheel-always generic.play
    (:require [generic.history :as hist]))

;;;
;; computer turn
;;;
(defn computer-turn?
  "Is it time for the computer to play?. Call this after player switch"
  [stings play]
  (and (not (game-over? stings play)) (= 1 (:players stings)) (= (:player stings) :b)))
g

(defn player-can-move?
  "Can a player move?"
  [stings play]
  (not (or (computer-turn? stings play) (game-over? stings play))))

(defn play-computer-turn
  "play computermove"
  [stings play optimal-outcome]
  (commit-play (optimal-outcome stings play)))

(defn schedule-computer-turn
  "schedule a computer play after a suitable delay"
  [think-time]
  (util/delayed-call think-time play-computer-turn))
