(ns ^:figwheel-always slippery.routing
  (:require [goog.events]
            [goog.history.EventType :as EventType]
            [secretary.core :as secretary :refer-macros [defroute]]
            [slippery.common :as common]
            [generic.game :as game])
  (:import goog.History))

(enable-console-print!)

;;
;; basic hash routing to configure some game options
;;

(secretary/set-config! :prefix "#")

(defn current-settings []
  (:settings @(:game common/Slippery)))

(defn dispatching

  ([]
   (dispatching (:viewer (current-settings))
                (:game-size (current-settings))
                (:coin-count (current-settings))
                (:limit (current-settings))
                (:players (current-settings))
                0))

  ([v]
   (dispatching v
                (:game-size (current-settings))
                (:coin-count (current-settings))
                (:limit (current-settings))
                (:players (current-settings))
                0))

  ([v gz]
   (dispatching v gz
                (:coin-count (current-settings))
                (:limit (current-settings))
                (:players (current-settings))
                0))

  ([v gz cc]
   (dispatching v gz cc
                (:limit (current-settings))
                (:players (current-settings))
                0))

  ([v gz cc l]
   (dispatching v gz cc l
                (:players (current-settings))
                0))

  ([v gz cc l p]
   (dispatching v gz cc l p 0))

  ([v gz cc l p f]
   (let [viewer v
         game-size (js.parseInt gz)
         coin-count (js.parseInt cc)
         limit (js.parseInt l)
         players (js.parseInt p)
         first-player (js.parseInt f)]
     (prn v ", " gz ", " cc ", " l ", " p ", " f)
     (when (and (common/check-game-size game-size)
                (common/check-coin-count coin-count)
                (common/check-limit limit)
                (common/check-players p))
       (swap! (:game common/Slippery) assoc-in [:settings :game-size] game-size)
       (swap! (:game common/Slippery) assoc-in [:settings :coin-count] coin-count)
       (swap! (:game common/Slippery) assoc-in [:settings :limit] limit)
       (swap! (:game common/Slippery) assoc-in [:settings :players] players)
       (let [fp (if (or (= 2 players)
                        (and (= 1 players)
                             (zero? first-player))) :a :b)]
         (swap! (:game common/Slippery) assoc-in [:play-state :player] fp))
       (common/switch-view viewer)
       (prn @(:game common/Slippery))
       (if (game/is-computer-turn? common/Slippery)
         (game/schedule-computer-turn common/Slippery))))))

(defroute
  full-island
  "/island/:game-size/:coin-count/:limit/:players/:first-player" {:as params}
  (dispatching :island
               (:game-size params)
               (:coin-count params)
               (:limit params)
               (:players params)
               (:first-player params)))

(defroute
  "/island/:game-size/:coin-count/:limit/:players" {:as params}
  (dispatching :island
               (:game-size params)
               (:coin-count params)
               (:limit params)
               (:players params)))

(defroute
  "/island/:game-size/:coin-count/:limit" {:as params}
  (dispatching :island
               (:game-size params)
               (:coin-count params)
               (:limit params)))

(defroute
  "/island/:game-size/:coin-count" {:as params}
  (dispatching :island
               (:game-size params)
               (:coin-count params)))

(defroute
  "/island/:game-size" {:as params}
  (dispatching :island
               (:game-size params)))

(defroute
  "/island" {:as params}
  (dispatching :island))

(defroute
  full-number
  "/number/:game-size/:coin-count/:limit/:players/:first-player" {:as params}
  (dispatching :number
               (:game-size params)
               (:coin-count params)
               (:limit params)
               (:players params)
               (:first-player params)))

(defroute
  "/number/:game-size/:coin-count/:limit/:players" {:as params}
  (dispatching :number
               (:game-size params)
               (:coin-count params)
               (:limit params)
               (:players params)))

(defroute
  "/number/:game-size/:coin-count/:limit" {:as params}
  (dispatching :number
               (:game-size params)
               (:coin-count params)
               (:limit params)))

(defroute
  "/number/:game-size/:coin-count" {:as params}
  (dispatching :number
               (:game-size params)
               (:coin-count params)))

(defroute
  "/number/:game-size" {:as params}
  (dispatching :number
               (:game-size params)))

(defroute
  "/number" {:as params}
  (dispatching :number))

(defroute
  "" {:as params}
  (dispatching))

(defroute
  "/" {:as params}
  (dispatching))

(defn params->url
  "convert parameters to a url"
  [viewer game-size limit players]
  (let [pmap {:game-size game-size :limit limit :players players}]
    (if (= viewer :number)
      (full-number pmap)
      (full-island pmap)
      )))

(defn save-settings
  "save settings in the url"
  []
  (let [settings (:settings @(:game common/Slippery))]
    (.replaceState js/history nil
                   (:title settings)
                   (params->url (:viewer settings) (:game-size settings)
                                (:limit settings) (:players settings)))))

;; history configuration.
;;
;; The invisible element "dummy" is needed to make goog.History reloadable by
;; figwheel. Without it we see
;; Failed to execute 'write' on 'Document':
;; It isn't possible to write into a document from an
;; asynchronously-loaded external script unless it is explicitly
;;
;; Note that this history handling must happen after route definitions for it
;; to kick in on initial page load.
;;
(let [h (History. false false "dummy")]
  (goog.events/listen h EventType/NAVIGATE #(secretary/dispatch! (.-token %)))
  (doto h (.setEnabled true)))
