(ns slippery.devcards
  (:require [devcards.core :as devcards]
            [generic.game :as game]
            [slippery.common :as common]
            )
  (:require-macros [devcards.core :refer [defcard deftest]]
                   [cljs.test :refer [is testing]]))

(deftest test-game-interface
  (testing "generic.game usage")
  (is (= true (game/is-over? common/Slippery))))
