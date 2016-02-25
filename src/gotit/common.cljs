(ns ^:figwheel-always gotit.common
    (:require [generic.util :as util]
              [generic.history :as hist]))

;; will be created to calculate svg screen transform
(defonce svg-point (atom false))

(defrecord Settings [title start target limit players])
(defonce settings (atom (Settings. "Got it!" 0 23 4 1)))

(defrecord PlayState [player state])
(def initial-play-state (PlayState. :a 0))
(defonce play-state (atom initial-play-state))

(defn reset-game
  []
  (hist/reset-history!)
  (reset! play-state initial-play-state))

(defn commit-play [new-play]
  (swap! play-state new-play))
