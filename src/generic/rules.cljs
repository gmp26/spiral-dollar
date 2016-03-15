(ns ^:figwheel-always generic.rules
    (:require [sprague-grundy.core :as core]))

;;;
;; A state is a vector of coin locations
;;
(defn- gaps [state]
  "but the gaps between the coins are more useful"
  (if (empty? state)
    []
    (reverse (cons (dec (first state)) (map dec (map - (rest state) state))))))

(defn- paired-gaps [state]
  (partition 2 (conj (vec (gaps state)) nil)))

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
     (heap-equivalent 1000 state)))

  ([limit state]
   (map (comp #(mod % (inc limit)) first) (paired-gaps state))))

(defn optimal-outcome
  "Return a winning move or a random small move"
  ([state]
   (optimal-outcome 1000 state))

  ([limit state]
   (let [heaps (heap-equivalent limit state)
         nimsum (apply bit-xor heaps)
         possible-moves (map - heaps (map (partial bit-xor nimsum) heaps))
         move (rand-nth
               (if (pos? nimsum)
                 (keep-indexed #(when (pos? %2)
                                  {:index (- (dec (count state)) (* 2 %1)) :move %2})
                               possible-moves)
                 (keep-indexed #(when (< %2 -1)
                                  {:index %1 :move 1})
                               (map - (cons 0 state) state))))]

     (update state (:index move) #(- % (:move move)))

     )))


;;;;;; generic stuff
