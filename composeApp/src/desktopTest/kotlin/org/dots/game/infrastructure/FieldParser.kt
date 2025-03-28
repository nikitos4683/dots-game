package org.dots.game.infrastructure

import org.dots.game.core.EMPTY_POSITION
import org.dots.game.core.FIRST_PLAYER_MARKER
import org.dots.game.core.Field
import org.dots.game.core.Player
import org.dots.game.core.Position
import org.dots.game.core.Rules
import org.dots.game.core.SECOND_PLAYER_MARKER
import kotlin.collections.iterator

object FieldParser {
    private val WHITESPACE_REGEX = Regex("\\s+")

    fun parseEmptyField(data: String): Field = parse(data, { width, height -> Rules(width, height, initialMoves = emptyList()) })

    fun parse(data: String, initializeRules: (Int, Int) -> Rules = { width, height -> Rules(width, height) }): Field {
        val lines = data.trim().split("\n", "\r\n")

        if (lines.isEmpty()) error("Field should have at least one cell")

        val movesWithNumberMap = mutableMapOf<Int, LightMove>()
        val movesWithoutNumbersList = mutableListOf<LightMove>()
        var width = 0

        for ((lineIndex, line) in lines.withIndex()) {
            val cells = line.trim().split(WHITESPACE_REGEX)
            if (cells.size > width) {
                width = cells.size
            }

            if (cells.isEmpty()) error("Empty line at $lineIndex")

            for ((cellIndex, cell) in cells.withIndex()) {
                if (cell.all { it == EMPTY_POSITION }) continue

                val player = when (cell.first()) {
                    FIRST_PLAYER_MARKER -> Player.First
                    SECOND_PLAYER_MARKER -> Player.Second
                    else -> {
                        error("Incorrect cell at ($cellIndex,$lineIndex). The marker should be either $FIRST_PLAYER_MARKER (first player) or $SECOND_PLAYER_MARKER (second player).")
                    }
                }

                val move = LightMove(Position(cellIndex + Field.OFFSET, lineIndex + Field.OFFSET), player)

                if (cell.length > 1) {
                    val parsedMoveNumber = cell.drop(1).toUIntOrNull()?.toInt()
                        ?: error("Incorrect cell move's number at ($cellIndex,$lineIndex).")

                    if (movesWithNumberMap.containsKey(parsedMoveNumber)) {
                        error("The move with number $parsedMoveNumber is already in use.")
                    }

                    movesWithNumberMap[parsedMoveNumber] = move
                } else {
                    movesWithoutNumbersList.add(move)
                }
            }
        }

        val allMoves = movesWithNumberMap.toSortedMap()
        var currentMoveNumber = 0
        for (move in movesWithoutNumbersList) {
            while (allMoves.containsKey(currentMoveNumber)) {
                currentMoveNumber++
            }
            allMoves[currentMoveNumber++] = move
        }

        val moves = buildList {
            var lastMoveNumber: Int? = null
            for ((moveNumber, move) in allMoves) {
                if (lastMoveNumber != null && moveNumber - lastMoveNumber > 1) {
                    error("The moves are missing: ${IntRange(lastMoveNumber + 1, moveNumber - 1)}")
                }

                add(move)
                lastMoveNumber = moveNumber
            }
        }

        val height = lines.size

        return Field(initializeRules(width, height)).apply {
            for ((index, move) in moves.withIndex()) {
                val position = move.position
                requireNotNull(makeMoveUnsafe(position, move.player), { "Can't make move #$index to $position" })
            }
        }
    }

    private data class LightMove(
        val position: Position,
        val player: Player,
    )
}