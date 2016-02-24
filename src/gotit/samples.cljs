(ns gotit.samples
  (:require [gotit.rules :refer [sample-followers sample-heaps]]
            [sprague-grundy.core :refer [Game]]))

(def gotit1 (Game. "Got it!" 0 23 4))
(def gotit2 (Game. "Got it!" 0 23 2))

(def gotit1-followers (sample-followers gotit1))
(def gotit2-followers (sample-followers gotit2))

(def gotit1-heaps (sample-heaps gotit1))
(def gotit2-heaps (sample-heaps gotit2))
