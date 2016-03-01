(ns ^:figwheel-always gotit.common
    (:require [generic.game :as game]
              [generic.util :as util]
              [generic.history :as hist]))

;; range validation
(def min-target 10)
(def max-target 40)
(def min-limit 1)
(def max-limit 6)
(def min-players 1)
(def max-players 2)

(defn check-target [t]
  (and (not (js.isNaN t)) (>= t min-target) (<= t max-target)))
(defn check-limit [l]
  (and (not (js.isNaN l))(>= l min-limit) (<= l max-limit)))
(defn check-players [p]
  (and (not (js.isNaN p))(>= p min-players) (<= p max-players)))

;; will be created to calculate svg screen transform
(defonce svg-point (atom false))

(defrecord Settings [title start target limit think-time players])
(def initial-settings (Settings. "Got it island!" 0 23 4 2000 1))

(defrecord PlayState [player feedback state])
(def initial-play-state (PlayState. :a "" 0))

(defrecord Game [game]
  game/IGame

  (is-over? [this]
    (let [gm @(:game this)]
      (when (= (:state (:play-state gm)) (:target (:settings gm)))
        (game/next-player this))))

  (get-status
    [this]
    (let [gm @(:game this)
          settings (:settings gm)
          play-state (:play-state gm)
          pa (= (:player play-state) :a)
          gover (game/is-over? this)
          over-class (if gover " pulsed" "")]
      (if (= (:players settings) 1)
        [over-class (cond
                      (= gover :a) :you-win
                      (= gover :b) :al-win
                      :else (if pa :yours :als))]
        [over-class (cond
                      (= gover :a) :b-win
                      (= gover :b) :a-win
                      :else (if pa :bs-turn :as-turn))])))

  (next-player [this]
    (if (= :a (:player (:play-state @(:game this)))) :b :a))

  (player-move [this move]
    (when (game/player-can-move? this)
      (hist/push-history! (:play-state @(:game this)))
                                        ; the move is the index of the pad the player clicked on
      (game/commit-play this move)
      #_(let [move 2]
          (swap! (:game this) assoc-in [:play-state :feedback] (str "You built "
                                                                    move " bridge"
                                                                    (if (> move 1) "s." "."))))))

  (commit-play [this new-play]
    (swap! (:game this) assoc :play-state (PlayState. (game/next-player this) "" new-play))
    (if (and (not (game/is-over? this)) (game/is-computer-turn? this))
      (game/schedule-computer-turn this)))

  (reset-game [this]
    (let [game-state (:game this)]
      (hist/empty-history!)
      (swap! game-state assoc :play-state (PlayState. :a "" 0))))

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
    [this]
    (hist/push-history! (:play-state @(:game this)))
    (game/commit-play this (game/optimal-outcome this)))

  (schedule-computer-turn
    [this]
    (let [move (- (game/optimal-outcome this) (:state (:play-state @(:game this))))]
      (swap! (:game this) assoc-in [:play-state :feedback] (str "Computer will build "
                                                                move " bridge"
                                                                (if (> move 1) "s." "."))))
    (util/delayed-call (:think-time (:settings @(:game this))) #(game/play-computer-turn this)))

  (followers
    [this state]
    (let [gm @(:game this)
          settings (:settings gm)
          target (:target settings)
          limit (:limit settings)]
      (if (set? state)
        (set (mapcat #(game/followers this %) state))
        (set (map #(+ state %) (range 1 (inc (min (- target state) limit))))))))

  (heap-equivalent
    [this]
    (let [gm @(:game this)
          settings (:settings gm)
          target (:target settings)
          limit (:limit settings)
          state (:state (:play-state gm))]
      (mod (- target state) (inc limit))))

  (optimal-outcome [this]
    (let [gm @(:game this)
          state (:state (:play-state gm))
          sum (game/heap-equivalent this)]
      (if (zero? sum)
        (inc state)
        (+ sum state)))))

(defonce Gotit (->Game (atom {:settings initial-settings
                              :play-state initial-play-state})))
