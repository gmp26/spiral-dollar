(ns generic.game)

(defprotocol IGame
  (is-over? [this])

  (reset-game [this])

  (commit-play [this new-play])
  )
