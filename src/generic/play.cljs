(ns ^:figwheel-always generic.play
    (:require [gotit.common :as common]
              [generic.game :as game]
              [generic.history :as hist]
              [generic.util :as util]))

;;;
;; computer turn
;;;
(defn computer-turn?
  "Is it time for the computer to play?. Call this after player switch"
  [stings play]
  (and (not (game/is-over? @common/Gotit)) (= 1 (:players stings)) (= (:player stings) :b)))


(defn player-can-move?
  "Can a player move?"
  [stings play]
  (not (or (computer-turn? stings play) (game/is-over? @common/Gotit))))

(defn play-computer-turn
  "play computermove"
  [optimal-outcome]
  (game/commit-play @common/Gotit optimal-outcome))

(defn schedule-computer-turn
  "schedule a computer play after a suitable delay"
  [think-time]
  (util/delayed-call think-time play-computer-turn))
