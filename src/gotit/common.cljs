(ns ^:figwheel-always gotit.common
    (:require [sprague-grundy.core :as core :refer [Game]]
              [generic.util :as util]))

(enable-console-print!)

(defonce svg-point (atom false))        ; will be created to calculate svg screen transform

(defrecord Settings [title start target limit players])
(defonce settings (atom (Settings. "Got it!" 0 23 4 1)))

(defrecord PlayState [player state])
(def initial-play-state (PlayState. :a 0))
(defonce play-state (atom initial-play-state))

(defn reset-game []
  (reset! play-state initial-play-state))


(defonce history (atom {:undo [initial-play-state] :redo []}))

;;;
;; move history handling
;;;
(defn reset-history!
  "reset history to start a new game"
  []
  (reset! history {:undo [initial-play-state] :redo []}))

(defn push-history!
  "Record game state in history"
  [play]
  (swap! history #({:undo (conj (:undo %) play)
                    :redo []})))


(defn undo!
  "pop history to the previous move"
  []
  (swap! history #(if (peek {:undo %})
                    {:undo (pop {:undo %}) :redo (conj {:redo %} (pop {:undo %}))}
                    %)))

(defn redo!
  "restore state of the next move if it exists"
  []
  (swap! history #(if (peek {:redo %})
                    {:redo (pop {:redo %}) :undo (conj {:undo %} (pop {:redo %}))}
                    %)))


(defn game-over? [stings play]
  (= (:state play) (:target stings)))


(defn commit-play [new-play]
  (swap! play-state new-play))

;;;
;; computer turn
;;;
(defn computer-turn?
  "Is it time for the computer to play?. Call this after player switch"
  [stings play]
  (and (not (game-over? stings play)) (= 1 (:players stings)) (= (:player stings) :b)))


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
