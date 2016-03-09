(ns ^:figwheel-always slippery.routing
    (:require [goog.events :as events]
              [goog.history.EventType :as EventType]
              [secretary.core :as secretary :refer-macros [defroute]]
              [slippery.common :as common]
              )
    (:import goog.History))

(enable-console-print!)

;;
;; basic hash routing to configure some game options
;;



(secretary/set-config! :prefix "#")


(defn dispatching [t l p v]
  (let [game-size (js.parseInt t)
        limit (js.parseInt l)
        players (js.parseInt p)
        viewer v]

    (prn "viewer " viewer)
    (when (and (common/check-game-size t) (common/check-limit l) (common/check-players p))
      (swap! (:game common/Slippery) assoc-in [:settings :game-size] game-size)
      (swap! (:game common/Slippery) assoc-in [:settings :limit] limit)
      (swap! (:game common/Slippery) assoc-in [:settings :players] players)
      (common/switch-view viewer)
      )))

(defroute full-island
  "/island/:game-size/:limit/:players" {:as params}
  (dispatching (:game-size params)
               (:limit params)
               (:players params)
               :island))

(defroute
  "/island/:game-size/:limit" {:as params}
  (dispatching (:game-size params)
               (:limit params)
               (:players (:settings @(:game common/Slippery)))
               :island))

(defroute
  "/island/:game-size" {:as params}
  (dispatching (:game-size params)
               (:limit (:settings @(:game common/Slippery)))
               (:players (:settings @(:game common/Slippery)))
               :island))

(defroute
  "/island" {:as params}
  (dispatching (:game-size (:settings @(:game common/Slippery)))
               (:limit (:settings @(:game common/Slippery)))
               (:players (:settings @(:game common/Slippery)))
               :island))


(defroute full-number
  "/number/:game-size/:limit/:players" {:as params}
  (dispatching (:game-size params)
               (:limit params)
               (:players params)
               :number))

(defroute
  "/number/:game-size/:limit" {:as params}
  (dispatching (:game-size params)
               (:limit params)
               (:players (:settings @(:game common/Slippery)))
               :number))

(defroute
  "/number/:game-size" {:as params}
  (dispatching (:game-size params)
               (:limit (:settings @(:game common/Slippery)))
               (:players (:settings @(:game common/Slippery)))
               :number))

(defroute
  "/number" {:as params}
  (dispatching (:game-size params)
               (:limit (:settings @(:game common/Slippery)))
               (:players (:settings @(:game common/Slippery)))
               :number))

(defroute
  "/:game-size/:limit/:players" {:as params}
  (dispatching (:game-size params)
               (:limit params)
               (:players params)
               :number))

(defroute
  "/:game-size/:limit" {:as params}
  (dispatching (:game-size params)
               (:limit params)
               (:players (:settings @(:game common/Slippery)))
               :number))

(defroute
  "/:game-size" {:as params}
  (dispatching (:game-size params)
               (:limit (:settings @(:game common/Slippery)))
               (:players (:settings @(:game common/Slippery)))
               :number))

(defroute
  "" {:as params}
  (dispatching (:game-size params)
               (:limit (:settings @(:game common/Slippery)))
               (:players (:settings @(:game common/Slippery)))
               :number))

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
