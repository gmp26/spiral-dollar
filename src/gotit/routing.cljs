(ns ^:figwheel-always gotit.routing
    (:require [goog.events :as events]
              [goog.history.EventType :as EventType]
              [secretary.core :as secretary :refer-macros [defroute]]
              [gotit.common :as common]
              )
    (:import goog.History))

(enable-console-print!)

;;
;; basic hash routing to configure some game options
;;



(secretary/set-config! :prefix "#")


(defn dispatching [t l p v]
  (let [target (js.parseInt t)
        limit (js.parseInt l)
        players (js.parseInt p)
        viewer v]

    (prn "viewer " viewer)
    (when (and (common/check-target t) (common/check-limit l) (common/check-players p))
      (swap! (:game common/Gotit) assoc-in [:settings :target] target)
      (swap! (:game common/Gotit) assoc-in [:settings :limit] limit)
      (swap! (:game common/Gotit) assoc-in [:settings :players] players)
      (swap! (:game common/Gotit) assoc-in [:settings :viewer] viewer)
      )))

(defroute
  "/island/:target/:limit/:players" {:as params}
  (dispatching (:target params)
               (:limit params)
               (:players params)
               :island))

(defroute
  "/island/:target/:limit" {:as params}
  (dispatching (:target params)
               (:limit params)
               (:players (:settings @(:game common/Gotit)))
               :island))

(defroute
  "/island/:target" {:as params}
  (dispatching (:target params)
               (:limit (:settings @(:game common/Gotit)))
               (:players (:settings @(:game common/Gotit)))
               :island))

(defroute
  "/island" {:as params}
  (dispatching (:target (:settings @(:game common/Gotit)))
               (:limit (:settings @(:game common/Gotit)))
               (:players (:settings @(:game common/Gotit)))
               :island))


(defroute
  "/number/:target/:limit/:players" {:as params}
  (dispatching (:target params)
               (:limit params)
               (:players params)
               :number))

(defroute
  "/number/:target/:limit" {:as params}
  (dispatching (:target params)
               (:limit params)
               (:players (:settings @(:game common/Gotit)))
               :number))

(defroute
  "/number/:target" {:as params}
  (dispatching (:target params)
               (:limit (:settings @(:game common/Gotit)))
               (:players (:settings @(:game common/Gotit)))
               :number))

(defroute
  "/number" {:as params}
  (dispatching (:target params)
               (:limit (:settings @(:game common/Gotit)))
               (:players (:settings @(:game common/Gotit)))
               :number))

(defroute
  "/:target/:limit/:players" {:as params}
  (dispatching (:target params)
               (:limit params)
               (:players params)
               :number))

(defroute
  "/:target/:limit" {:as params}
  (dispatching (:target params)
               (:limit params)
               (:players (:settings @(:game common/Gotit)))
               :number))

(defroute
  "/:target" {:as params}
  (dispatching (:target params)
               (:limit (:settings @(:game common/Gotit)))
               (:players (:settings @(:game common/Gotit)))
               :number))

(defroute
  "" {:as params}
  (dispatching (:target params)
               (:limit (:settings @(:game common/Gotit)))
               (:players (:settings @(:game common/Gotit)))
               :number))

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
