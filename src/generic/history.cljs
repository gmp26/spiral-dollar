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
