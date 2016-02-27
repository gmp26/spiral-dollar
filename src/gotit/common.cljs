(ns ^:figwheel-always gotit.common
    (:require [generic.game :as game]
              [generic.util :as util]
              [generic.history :as hist]))

;; will be created to calculate svg screen transform
(defonce svg-point (atom false))

(defrecord Settings [title start target limit players])
(def initial-settings (Settings. "Got it!" 0 23 4 1))
;;(defonce settings (atom (Settings. "Got it!" 0 23 4 1)))

(defrecord PlayState [player reached state])
(def initial-play-state (PlayState. :a [:a :b :b :b] 0))
;;(defonce play-state (atom initial-play-state))

(defrecord Game [settings play-state]
  game/IGame

  (is-over? [this]
    (= (:state (:play-state @this) (:target (:settings @this)))))

  (reset-game [this]
    (hist/reset-history!)
    (swap! this assoc :play-state initial-play-state)
    )

  (commit-play [this new-play]
    (swap! this assoc :play-state new-play))

  )

(defonce Gotit (atom (->Game
                      initial-settings
                      initial-play-state)))
