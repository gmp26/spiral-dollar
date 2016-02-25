(ns ^:figwheel-always generic.history)

;;;
;; move history handling
;;;
(defonce history (atom {:undo [] :redo []}))

(defn empty-history!
  "Empty history at start of a new game then call push history to enter the inital state."
  []
  (reset! history {:undo [] :redo []}))

(defn reset-history!
  "reset history to start a new game. Will not change the initial state in {:undo 0}"
  []
  (swap! history #({:undo [{:undo %} 0] :redo []})))

(defn push-history!
  "Record game state in history"
  [play]
  (swap! history #({:undo (conj (:undo %) play)
                    :redo []})))

(defn undo!
  "pop history to the previous move"
  []
  (swap! history
         #(if (peek {:undo %})
            {:undo (pop {:undo %}) :redo (conj {:redo %} (pop {:undo %}))}%)))

(defn redo!
  "restore state of the next move if it exists"
  []
  (swap! history
         #(if (peek {:redo %})
            {:redo (pop {:redo %}) :undo (conj {:undo %} (pop {:redo %}))}%)))
