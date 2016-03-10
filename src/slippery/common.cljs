(ns ^:figwheel-always slippery.common
    (:require [generic.game :as game]
              [generic.util :as util]
              [generic.history :as hist]))

;; range validation
(def min-game-size 10)
(def max-game-size 40)
(def min-limit 1)
(def max-limit max-game-size)
(def min-players 1)
(def max-players 2)

(defn check-game-size [t]
  (and (not (js.isNaN t)) (>= t min-game-size) (<= t max-game-size)))
(defn check-limit [l]
  (and (not (js.isNaN l))(>= l min-limit) (<= l max-limit)))
(defn check-players [p]
  (and (not (js.isNaN p))(>= p min-players) (<= p max-players)))

;; drag state
(defonce drag-state (atom {:svg-point nil
                           :drag-start nil}))

(defrecord Settings [title game-size coin-count limit think-time players viewer])
(def initial-settings (Settings. "Silver Dollar" 40 6 4 2000 1 :number))

(defn random-state [{:keys [:game-size :coin-count]}]
  (reduce #(if (< (count %1) coin-count) (conj %1 %2) %1) (sorted-set) (map #(inc (rand-int game-size)) (range (+ 5 coin-count)))))

#_(defn random-state [{:keys [:game-size :coin-count]}]
  (take count (into (sorted-set) (into #{} (map #(inc (rand-int game-size)) (range (* 1 count)))))))

#_(defn random-state [{:keys [:game-size :coin-count]}]
  (take count (apply sorted-set (map #(inc (rand-int game-size)) (range count)))))

(defrecord PlayState [player feedback state])
(def initial-play-state (PlayState. :a "" (random-state initial-settings)))



(defrecord Game [game]
  game/IGame

  (is-over? [this]
    (let [gm @(:game this)]
      (if (= (:state (:play-state gm)) [])
        (:player (:play-state gm))
        false)))

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

  (commit-play [this new-play]
    ;; todo
    (swap! (:game this) assoc-in [:play-state :state] new-play)
    (when (not (game/is-over? this))
      (swap! (:game this) assoc-in [:play-state :player] (game/next-player this))
      (if (game/is-computer-turn? this)
        (game/schedule-computer-turn this))))

  (reset-game [this]
    (let [game-state (:game this)]
      (hist/empty-history!)
      (swap! game-state assoc :play-state (PlayState. :a "" [3 10 21 30]))))

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
    ;; todo
    (let [move (- (game/optimal-outcome this) (:state (:play-state @(:game this))))]
      (swap! (:game this) assoc-in [:play-state :feedback] (str "Computer goes " move)))
    (util/delayed-call (:think-time (:settings @(:game this))) #(game/play-computer-turn this)))

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
    (prn "todo: heap-equivalent")
    (let [state (:state (:play-state @(:game this)))
          limit (:limit (:settings @(:game this)))
          ]
      (if (empty? state)
        '(0)
        (let [gaps (reverse (cons (first state) (map dec (map - (rest state) state))))
              paired-gaps (partition 2 (conj (vec gaps) nil))]
          (if (or (nil? limit) (zero? limit))
            (map first paired-gaps)
            (map (comp #(mod % (inc limit)) first) paired-gaps))))))

  (optimal-outcome [this]
    (let [gm @(:game this)
          state (:state (:play-state gm))
          sum (game/heap-equivalent this)]
      (if (zero? sum)
        (prn "todo: make random move")
        (prn "todo: make the best move")
        ))))

(defonce Slippery (->Game (atom {:settings initial-settings
                              :play-state initial-play-state})))

(defn switch-view
  "switch game view"
  [key]
  (swap! (:game Slippery) assoc-in [:settings :viewer] key)
  (swap! (:game Slippery) assoc-in [:settings :title]
         (if (= key :number) "Silver Dollar" "Slippery Snail")))
