(ns generic.game)

(defprotocol IGame
  (is-over? [this]
    "is the game over? return winner if true")

  (get-status [this]
    "get the status of the game (usually as a keyword)")

  (next-player [this]
    "return the next player")

  (player-move [this move]
    "make a player move")

  (reset-game [this]
    "reset game to starting position")

  (commit-play [this new-play]
    "commit a move")

  (is-computer-turn? [this]
    "is it the computer's turn?")

  (player-can-move? [this]
    "can the player move?")

  (play-computer-turn [this]
    "really play the computer's turn")

  (schedule-computer-turn [this]
    "schedule the computer's turn after a deley")

  (followers [this state]
    "returns a set of states that may follow given state")

  (heap-equivalent [this]
    "The nim-sum of the game - nim single heap equivalent of a gotit state")

  (optimal-outcome [this]
    "If we are in an N state returns a winning move. If we are in a P state, returns a small move.")
  )
