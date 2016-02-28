(ns ^:figwheel-always gotit.common
    (:require [generic.game :as game]
              [generic.util :as util]
              [generic.history :as hist]))

;; will be created to calculate svg screen transform
(defonce svg-point (atom false))

(defrecord Settings [title start target limit players])
(def initial-settings (Settings. "Got it!" 0 23 4 2))

(defrecord PlayState [player state])
(def initial-play-state (PlayState. :a 0))

(defrecord Game [game]
  game/IGame

  (is-over? [this]
    (let [gm @(:game this)]
      (when (= (:state (:play-state gm)) (:target (:settings gm)))
        (game/next-player this))))

  (next-player [this]
    (if (= :a (:player (:play-state @(:game this)))) :b :a))

  (commit-play [this new-play]
    (swap! (:game this) assoc :play-state (PlayState. (game/next-player this) new-play)))

  (reset-game [this]
    (let [game-state (:game this)]
      (hist/empty-history!)
      (swap! game-state assoc :play-state initial-play-state)))

  (is-computer-turn?
    [this]
    (let [gm @(:game this)]
      (and (not (game/is-over? this))
           (= 1 (:players (:settings gm)))
           (= (:player (:play-state gm)) :b))))

  (player-can-move?
    [this]
    (not (or (game/is-computer-turn? this) (game/is-over? this))))

  (play-computer-turn
    [this optimal-outcome]
    (game/commit-play this optimal-outcome))

  (schedule-computer-turn
    [this think-time optimal-outcome]
    (util/delayed-call think-time (game/play-computer-turn this optimal-outcome)))

  )

(defonce Gotit (->Game (atom {:settings initial-settings
                              :play-state initial-play-state})))
