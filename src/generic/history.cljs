(ns ^:figwheel-always generic.history)

;;;
;; move history handling
;;;
(defonce history (atom {:undo [] :redo []}))

(defn empty-history!
  "Empty history at start of a new game then call push history to enter the inital state."
  []
  (reset! history {:undo [] :redo []}))

(defn push-history!
  "Record game state in history"
  [play]
  (swap! history
         #(assoc %
                 :undo (conj (:undo %) play)
                 :redo [])))

(defn push-future!
  "Record game state in future"
  [play]
  (swap! history
         #(assoc %
                 :redo (conj (:redo %) play)
                 :undo [])))

(defn peek-history! []
  "return the top of the undo stack, or nil"
  (peek (:undo @history)))

(defn peek-future! []
  "return the top of the redo stack, or nil"
  (peek (:redo @history)))

#_(defn undo!
  "pop history to the previous move"
  []
  (swap! history
         #(assoc %
                 :undo (if-let [u (peek (:undo %))] (pop (:undo %)) [])
                 :redo (if-let [u (peek (:undo %))] (conj (:redo %) u) (:redo %)))))

#_(defn redo!
  "restore state of the next move if it exists"
  []
  (swap! history
         #(assoc %
                 :redo (if-let [u (peek (:redo %))] (pop (:redo %)) [])
                 :undo (if-let [u (peek (:redo %))] (conj (:undo %) u) (:undo %)))))


(defn undo!
  [state]
  (if-let [undone (peek (:undo @history))]
    (do (swap! history #(assoc %
                               :redo (conj (:redo %) state)
                               :undo (pop (:undo %)))) undone) state))

(defn redo!
  [state]
  (if-let [redone (peek (:redo @history))]
    (do (swap! history #(assoc %
                               :undo (conj (:undo %) state)
                               :redo (pop (:redo %)))) redone) state))
