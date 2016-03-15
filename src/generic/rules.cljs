(ns ^:figwheel-always generic.rules
    (:require [sprague-grundy.core :as core]))

;;;
;; A state is a vector of coin locations
;;
(defn- gaps [state]
  "but the gaps between the coins are more useful"
  (reverse (cons (dec (first state)) (map dec (map - (rest state) state)))))

(defn- location-follower
  "followers are the states that can follow individual state(s) according to the game rules."
  [previous-loc loc]
  (range (inc previous-loc) loc))

(defn- unlimited-moves [state]
  (filter #(not ((set state) %)) (range 1 (apply max state)))
  )

(defn- move-location
  "given a move (a gap location) find the location of the counter that can move there"
  [state move]
  (first (filter #(> % move) state)))

(defn- limited-moves [limit state]
  "return those moves that are less than the move limit"
  (filter (fn [move] (<= (- (move-location state move) move) limit))  (unlimited-moves state))
  )

;;;
;; Public
;;;
(defn make-move
  "fold the move (however represented) into the play state (however represented)"
  [state move]
  (drop-while zero? (map #(if (= % (move-location state move)) move %) state))
  )

(defn followers
  "followers are the states that can follow this state (or set of states) according to the game rules."
  [state]
  (if (set? state)
    (set (mapcat followers state))
    (map #(make-move state %) (unlimited-moves state))
    ))

(defn limited-followers
  "followers are the states that can follow this state (or set of states) according to the game rules."
  [limit state]
  (if (set? state)
    (set (mapcat limited-followers limit state))
    (map #(make-move state %) (limited-moves limit state))))

(defn sample-followers
  "return the correct followers function for the given state"
  [sample]
  (if (:limit sample)
    (partial limited-followers (:limit sample))
    (partial followers)
    ))

;;;
;; The state of a dollar is the vector of locations of coins
;;;
; e.g.
; (defonce dollar (atom [4 8 13 18]))
(defn heap-equivalent
  "Returns a seq of equivalent nim heaps for a dollar game-state"

  ([state]
   (if (empty? state)
     '(0)
     (map first (partition 2 (conj (vec (gaps state)) nil))))
   )

  ([limit state]
   (if (empty? state)
     '(0)
     (map (comp #(mod % (inc limit)) first) (partition 2 (conj (vec (gaps state)) nil))))))


(defn sample-heaps
  "Returns a curried function of state giving only the heap-equivalent"
  [sample]
  (fn [state]
    (if-let [lim (:limit sample)]
      (heap-equivalent lim state)
      (heap-equivalent state)))
  )


;;;;;; generic stuff
