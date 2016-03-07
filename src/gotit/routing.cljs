(ns ^:figwheel-always gotit.routing
    (:require [goog.events :as events]
              [goog.history.EventType :as EventType]
              [secretary.core :as secretary :refer-macros [defroute]]
              [gotit.common :as common]
              [gotit.spiral-view :as spiral]
              )
    (:import goog.History))

(enable-console-print!)

(prn (spiral/->Spiral-view.))

(defonce Game-view (atom (spiral/Spiral-view.)))

;;
;; basic hash routing to configure some game options
;;

(secretary/set-config! :prefix "#")


(defn dispatching [t l p]
  (let [target (js.parseInt t)
        limit (js.parseInt l)
        players (js.parseInt p)]
    (when (and (common/check-target t) (common/check-limit l) (common/check-players p))
      (swap! (:game common/Gotit) assoc-in [:settings :target] target)
      (swap! (:game common/Gotit) assoc-in [:settings :limit] limit)
      (swap! (:game common/Gotit) assoc-in [:settings :players] players))))

(defroute
  "/:target/:limit/:players" {:as params}
  (dispatching (:target params) (:limit params) (:players params))
  )

(defroute
  "/:target/:limit" {:as params}
  (dispatching (:target params) (:limit params) (:players (:settings @(:game common/Gotit))))
  )

(defroute
  "/:target" {:as params}
  (dispatching (:target params)
               (:limit (:settings @(:game common/Gotit)))
               (:players (:settings @(:game common/Gotit)))))

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
