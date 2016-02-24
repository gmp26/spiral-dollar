(ns generic.util)

;;;;; could move this stuff to a generic common

(defn el
  "get dom element by id"
  [id]
  (.getElementById js/document id))

(defn delayed-call
  "Call f, optionally with arguments xs, after ms milliseconds"
  [ms f & xs]
  (js/setTimeout #(apply f xs) ms))
