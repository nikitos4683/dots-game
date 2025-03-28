import org.dots.game.core.EMPTY_POSITION
import org.dots.game.core.EMPTY_TERRITORY_MARKER
import org.dots.game.core.Field
import org.dots.game.core.Field.Companion.OFFSET
import org.dots.game.core.Position
import org.dots.game.core.TERRITORY_EMPTY_MARKER
import org.dots.game.core.playerMarker

fun Field.render(printNumbers: Boolean = true, padding: Int = Int.MAX_VALUE, printCoordinates: Boolean = true, debugInfo: Boolean = false): String {
    var minX = width
    var maxX = OFFSET
    var minY = height
    var maxY = OFFSET

    var maxMarkerLength = 0
    val positionToMoveResult = moveSequence.associateBy { it.position }

    val dotsRepresentation = Array(realWidth) { x ->
        Array(realHeight) { y ->
            val position = Position(x, y)
            val state = position.getState()
            val isTerritory = debugInfo && state.checkTerritory()
            val isPlaced = state.checkPlaced()

            buildString {
                if (isTerritory) {
                    append(playerMarker.getValue(state.getTerritoryPlayer()))
                }
                if (state.checkPlaced()) {
                    append(playerMarker.getValue(state.getPlacedPlayer()))
                } else if (isTerritory) {
                    append(TERRITORY_EMPTY_MARKER)
                }
                val emptyBaseAtPosition = emptyBasePositionsSequence[position]
                if (debugInfo && emptyBaseAtPosition != null) {
                    require(!isTerritory && !isPlaced)
                    append(EMPTY_TERRITORY_MARKER)
                    append(playerMarker.getValue(emptyBaseAtPosition.player))
                }
                if (isEmpty()) {
                    append(
                        when (x) {
                            0 -> {
                                when (y) {
                                    0 -> '┌'
                                    realHeight - 1 -> '└'
                                    else -> '│'
                                }
                            }

                            realWidth - 1  -> {
                                when (y) {
                                    0 -> '┐'
                                    realHeight - 1 -> '┘'
                                    else -> '│'
                                }
                            }

                            else -> {
                                when (y) {
                                    0, realHeight - 1 -> '─'
                                    else -> EMPTY_POSITION
                                }
                            }
                        }
                    )
                } else {
                    if (printNumbers) {
                        positionToMoveResult[position]?.number?.let { append(it) }
                    }

                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }.also {
                if (it.length > maxMarkerLength) {
                    maxMarkerLength = it.length
                }
            }
        }
    }

    val firstX = maxOf(minX.toLong() - padding, 0).toInt()
    val firstY = maxOf(minY.toLong() - padding, 0).toInt()
    val lastX = minOf(maxX.toLong() + padding, (realWidth - 1).toLong()).toInt()
    val lastY = minOf(maxY.toLong() + padding, (realHeight - 1).toLong()).toInt()

    if (printCoordinates) {
        lastX.toString().length.let { if (it > maxMarkerLength) maxMarkerLength = it }
    }

    return buildString {
        fun StringBuilder.appendWithPadding(str: String, x: Int) {
            append(str)
            if (x < lastX) {
                (0 until maxMarkerLength - str.length + 1).forEach { _ -> append(' ') }
            }
        }

        for (y in firstY..lastY) {
            if (printCoordinates) {
                if (y == firstY) {
                    appendWithPadding("\\", 0)
                    for (x in firstX..lastX) {
                        appendWithPadding(x.toString(), x)
                    }
                    append('\n')
                }
                appendWithPadding(y.toString(), 0)
            }
            for (x in firstX..lastX) {
                appendWithPadding(dotsRepresentation[x][y], x)
            }
            if (y < lastY) {
                append('\n')
            }
        }
    }
}