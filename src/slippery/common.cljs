(ns ^:figwheel-always slippery.common
    (:require [generic.game :as game]
              [generic.rules :as rules]
              [generic.util :as util]
              [generic.history :as hist]))

;; range validation
(def min-game-size 10)
(def max-game-size 40)
(def min-coin-count 2)
(def max-coin-count 6)
(def min-limit 1)
(def max-limit max-game-size)
(def min-players 1)
(def max-players 2)

(defn check-game-size [gz]
  (and (not (js.isNaN gz)) (>= gz min-game-size) (<= gz max-game-size)))
(defn check-coin-count [cc]
  (and (not (js.isNaN cc))(>= cc min-coin-count) (<= cc max-coin-count)))
(defn check-limit [l]
  (and (not (js.isNaN l))(>= l min-limit)
       (or (= l 1000) (<= l max-limit))))
(defn check-players [p]
  (and (not (js.isNaN p))(>= p min-players) (<= p max-players)))

;; drag state
(defonce svg-point (atom nil))
(defonce drag-state (atom {:drag-start nil :state []}))

(defrecord Settings [title game-size coin-count limit think-time players viewer])
(def initial-settings (Settings. "Silver Dollar" 20 6 4 2000 1 :number))

(defn random-state [{:keys [:game-size :coin-count]}]
  (vec (reduce #(if (< (count %1) coin-count) (conj %1 %2) %1)
               (sorted-set)
               (map #(inc (rand-int (dec game-size))) (range (+ 5 coin-count))))))

(defrecord PlayState [player feedback state])
(def initial-play-state (PlayState. :a "" (random-state initial-settings)))

(defn is-number?
  "is this the snail/island game of the crane/number game?"
  [game]
  (= :number (:viewer (:settings @(:game game)))))

(defn- move-location [state move]
  (last (filter #(< % move) state)))

(defn make-move
  "move the last state location smaller than the move "
  [game-size state move]
  (vec (filter #(< % game-size) (map #(if (= % (move-location state move)) move %) state)))
  )

(defn viewer-state [game state]
  (vec (reverse (map #(- (inc (:game-size (:settings game))) %) state)))
  )

#_(defn viewer-dir [game]
  (if (= :number (:viewer (:settings game)))
    (partial +) (partial -)))

(defrecord Game [game]
  game/IGame

  (is-over? [this]
    (let [gm @(:game this)
          play-state (:play-state gm)
          state (:state play-state)
          player (:player play-state)]
      (if (is-number? this)
        (if (not-any? #(not= -1 %) (map - (cons 0 state) state)) player false)
        (if (#{[] [(:game-size (:settings gm))]} state) player false))))

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
      (game/commit-play this move)))

  (commit-play [this move]
    (when (not (is-number? this))
      (let [game @(:game this)
            new-state (make-move (:game-size (:settings game)) (:state (:play-state game)) move)]
        (swap! (:game this) assoc-in [:play-state :state] new-state)))

    ;; in the number version the new state will already be realised by the drag operation
    (when (not (game/is-over? this))
      (swap! (:game this) assoc-in [:play-state :player] (game/next-player this))
      (if (game/is-computer-turn? this)
        (game/schedule-computer-turn this)))

    )

  (reset-game [this]
    (let [game-state (:game this)]
      (hist/empty-history!)
      (swap! game-state assoc :play-state (PlayState. :a "" (random-state (:settings @(:game this)))))))

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
    (let [o-o (game/optimal-outcome this)
          ;new-state (if (is-number? this) o-o (vec (filter #(< % (:game-size (:settings @(:game this)))) o-o)))
          new-state o-o]
      (prn "o-o = " o-o " new-state = " new-state)
      (util/delayed-call (:think-time (:settings @(:game this)))
                         #(do
                           (swap! (:game this) assoc-in [:play-state :state] new-state)
                           (when (not (game/is-over? this))
                             (swap! (:game this) assoc-in [:play-state :player] (game/next-player this)))))))

  (followers
    [this state]
    (let [gm @(:game this)
          settings (:settings gm)
          game-size (:game-size settings)
          limit (:limit settings)]
      (if (set? state)
        (set (mapcat #(game/followers this %) state))
        (set (map #(+ state %) (range 1 (inc (min (- game-size state) limit))))))))

  (heap-equivalent
    [this]
    (prn "common-h-e")
    (let [gm @(:game this)]
      (rules/heap-equivalent (:limit (:settings gm)) (viewer-state gm (:state (:play-state gm)))))
    )

  (optimal-outcome [this]
    (let [gm @(:game this)]
      (if (is-number? this)
        (rules/optimal-outcome (:limit (:settings gm))
                               (:state (:play-state gm))
                               )
        (viewer-state gm (rules/optimal-outcome (:limit (:settings gm))
                                                (viewer-state gm (:state (:play-state gm)))
                                                ))))))

(defonce Slippery (->Game (atom {:settings initial-settings
                              :play-state initial-play-state})))

(defn switch-view
  "switch game view"
  [key]
  (swap! (:game Slippery) assoc-in [:settings :viewer] key)
  (swap! (:game Slippery) assoc-in [:settings :title]
         (if (= key :number) "Silver Dollar" "Slippery Snail")))