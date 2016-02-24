(ns ^:figwheel-always gotit.common
  (:require [sprague-grundy.core :as core :refer [Game]]))

(enable-console-print!)

(defonce svg-point (atom false))        ; will be created to calculate svg screen transform

(defrecord Settings [title start target limit players])
(defonce settings (atom (Settings. "Got it!" 0 23 4 1)))

(defrecord PlayState [player state])
(def initial-play-state (PlayState. :a 0))
(defonce play-state (atom initial-play-state))

(defn reset-game []
  (reset! play-state initial-play-state))




(defonce history (atom [0]))

(defn game-over? [stings play]
  (= (:state play) (:target stings)))
