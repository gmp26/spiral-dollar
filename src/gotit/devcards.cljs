(ns gotit.devcards
  (:require [devcards.core :as devcards]
            [generic.game :as game]
            )
  (:require-macros [devcards.core :refer [defcard deftest]]
                   [cljs.test :refer [is testing]]))


(defrecord Game [settings play-state]
  game/IGame
  (is-over? [this]
    (= (:settings this) (:play-state this)))

  (reset-game [this]
    prn "reset game")

  (commit-play [this new-play]
    prn "commit-play")
  )


(deftest test-game-interface
  (testing "generic.game usage")
  (is (= true (game/is-over? (->Game 1 1)))))
