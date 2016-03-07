(ns generic.viewer)

;;;
;; Generic game viewer. Useful when the same model drives different viewer implementations
;;;

(defprotocol IViewer
  "Define an interface which implements a generic game viewer"

  ;; get a status message given a player/move status key
  (get-message [this status])

  ;; get a fill colour dependent on the player/move status key
  (get-fill [this status])

  ;; draw the game board using an optional configuration parameter
  (game-viewer [this config])

  ;; display game help
  (help-viewer [this])
  )
