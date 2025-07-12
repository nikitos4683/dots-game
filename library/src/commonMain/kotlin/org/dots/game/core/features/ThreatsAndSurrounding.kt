package org.dots.game.core.features

import org.dots.game.core.Field
import org.dots.game.core.Player
import org.dots.game.core.Position

fun Field.getOneMoveCapturingAndBasePositions(): OneMoveCapturingAndBasePositionsFeature {
    val oneMoveCapturingPositions = hashMapOf<Position, Player>()
    val oneMoveBasePositions = hashMapOf<Position, Player>()

    for (x in 1..width) {
        for (y in 1..height) {
            val position = Position(x, y)

            fun collectCapturingAndPotentiallyBasePositions(player: Player) {
                with (this) {
                    val state = position.getState()
                    if (state.checkWithinEmptyTerritory()) {
                        val emptyTerritoryPlayer = state.getEmptyTerritoryPlayer()
                        oneMoveBasePositions[position] = (oneMoveBasePositions[position] ?: Player.None) + emptyTerritoryPlayer
                        // Optimization: the dot placed into own empty territory never captures anything
                        if (emptyTerritoryPlayer == player) return
                    }

                    val moveResult = makeMove(position, player)
                    if (moveResult != null) {
                        unmakeMove()

                        if (moveResult.bases != null) {
                            if (moveResult.bases.any { it.isReal && it.player == player }) {
                                oneMoveCapturingPositions[position] = (oneMoveCapturingPositions[position] ?: Player.None) + player
                            }

                            for (base in moveResult.bases) {
                                for ((position, _) in base.previousPositionStates) {
                                    oneMoveBasePositions[position] = (oneMoveBasePositions[position] ?: Player.None) + base.player
                                }
                            }
                        }
                    }
                }
            }

            collectCapturingAndPotentiallyBasePositions(Player.First)
            collectCapturingAndPotentiallyBasePositions(Player.Second)
        }
    }

    return OneMoveCapturingAndBasePositionsFeature(oneMoveCapturingPositions, oneMoveBasePositions)
}

data class OneMoveCapturingAndBasePositionsFeature(
    val capturingPositions: Map<Position, Player>,
    val basePositions: Map<Position, Player>,
)



