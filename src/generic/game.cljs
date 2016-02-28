(ns generic.game)

(defprotocol IGame
  (is-over? [this]
    "is the game over? return winner if true")

  (next-player [this]
    "return the next player")

  (reset-game [this]
    "reset game to starting position")

  (commit-play [this new-play]
    "commit a move")

  (is-computer-turn? [this]
    "is it the computer's turn?")

  (player-can-move? [this]
    "can the player move?")

  (play-computer-turn [this optimal-outcome]
    "really play the computer's turn")

  (schedule-computer-turn [this think-time optimal-outcome]
    "schedule the computer's turn after a deley")
  )
