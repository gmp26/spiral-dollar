(ns generic.util)


(defn el
  "get dom element by id"
  [id]
  (.getElementById js/document id))
