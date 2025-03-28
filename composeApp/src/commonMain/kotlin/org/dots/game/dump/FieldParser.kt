package org.dots.game.dump

import org.dots.game.core.EMPTY_POSITION
import org.dots.game.core.FIRST_PLAYER_MARKER
import org.dots.game.core.Field
import org.dots.game.core.Player
import org.dots.game.core.Position
import org.dots.game.core.Rules
import org.dots.game.core.SECOND_PLAYER_MARKER

object FieldParser {
    private val WHITESPACE_REGEX = Regex("\\s+")

    fun parseFieldWithNoInitialMoves(data: String): Field = parse(data, { width, height ->
        Rules(
            width,
            height,
            initialMoves = emptyList()
        )
    })

    fun parse(data: String, initializeRules: (Int, Int) -> Rules = { width, height -> Rules(width, height) }): Field {
        val lines = data.trim().split("\n", "\r\n")

        if (lines.isEmpty()) error("Field should have at least one cell")

        val movesWithNumberMap = mutableMapOf<Int, LightMove>()
        val movesWithoutNumberList = mutableListOf<LightMove>()
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

                val move = LightMove(Position(cellIndex + Field.Companion.OFFSET, lineIndex + Field.Companion.OFFSET), player)

                if (cell.length > 1) {
                    val parsedMoveNumber = cell.drop(1).toUIntOrNull()?.toInt()
                        ?: error("Incorrect cell move's number at ($cellIndex,$lineIndex).")

                    if (movesWithNumberMap.containsKey(parsedMoveNumber)) {
                        error("The move with number $parsedMoveNumber is already in use.")
                    }

                    movesWithNumberMap[parsedMoveNumber] = move
                } else {
                    movesWithoutNumberList.add(move)
                }
            }
        }

        val sortedMoves = movesWithNumberMap.entries.sortedBy { it.key }
        var moveNumberForUnnumberedMoves = 0
        var previousMoveNumber = -1

        val allMoves = buildList {
            for ((number, move) in sortedMoves) {
                val maxMoveCountToInsert = number - previousMoveNumber - 1
                if (maxMoveCountToInsert > 0) {
                    val moveCountToInsert =
                        minOf(movesWithoutNumberList.size - moveNumberForUnnumberedMoves, maxMoveCountToInsert)
                    (0 until moveCountToInsert).forEach { _ ->
                        add(movesWithoutNumberList[moveNumberForUnnumberedMoves++])
                    }
                    if (number - size > 0) {
                        error("The moves are missing: ${IntRange(size, number - 1)}")
                    }
                }
                add(move)
                previousMoveNumber = number
            }

            while (moveNumberForUnnumberedMoves < movesWithoutNumberList.size) {
                add(movesWithoutNumberList[moveNumberForUnnumberedMoves++])
            }
        }

        val height = lines.size

        return Field(initializeRules(width, height)).apply {
            for ((index, move) in allMoves.withIndex()) {
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