package org.dots.game.core

import org.dots.game.core.OffsetHelpers.OFFSET_Y_BITS_COUNT
import org.dots.game.core.OffsetHelpers.nextPositionIndex
import org.dots.game.core.OffsetHelpers.positionOffset

object OffsetHelpers {
    const val OFFSET_X_BITS_COUNT = 2
    const val OFFSET_Y_BITS_COUNT = 2

    internal val nextPositionIndex: IntArray = IntArray(1 shl (OFFSET_X_BITS_COUNT + OFFSET_Y_BITS_COUNT)) { -1 }
    internal val positionOffset: Array<Offset> = buildList {
        var positionOffsetIndex = 0

        fun calculate(diffX: Int, diffY: Int) {
            var index = calculateWalkIndex(diffX, diffY)
            nextPositionIndex[index] = (positionOffsetIndex + 1) % 8
            add(Offset(diffX, diffY))
            positionOffsetIndex++
        }

        fun calculateMainLoop() {
            calculate(-1, -1)
            calculate(0, -1)
            calculate(+1, -1)
            calculate(+1, 0)
            calculate(+1, +1)
            calculate(0, +1)
            calculate(-1, +1)
        }

        calculateMainLoop()
        calculate(-1, 0)

        calculateMainLoop() // Calculate positions again to avoid extra range check
    }.toTypedArray()
}

fun Position.linkedWith(other: Position): Boolean {
    return squareDistanceTo(other) <= 2
}

fun Position.squareDistanceTo(other: Position): Int {
    val diffX = x - other.x
    val diffY = y - other.y
    return diffX * diffX + diffY * diffY
}

fun Position.getSquare(other: Position): Int {
    return y * other.x - other.y * x
}

/**
 * Returns the next position in clockwise direction relative the passed @param[other]
 *
 * ```
 *   x * .   . x *   . . x   . . .   . . .   . . .   . . .   * . .
 *   . o .   . o .   . o *   . o x   . o .   . o .   * o .   x o .
 *   . . .   . . .   . . .   . . *   . * x   * x .   x . .   . . .
 * ```
 *  - o @param[Position]
 *  - x @param[other]
 *  - * @return
 */
internal inline fun Position.clockwiseWalk(other: Position, action: (Position) -> Boolean): Boolean {
    val (x, y) = this
    var index = nextPositionIndex[calculateWalkIndex(other.x - x, other.y - y)]
    var iteration = 0

    while (iteration < 8) {
        if (!action(positionOffset[index++].let { Position(x + it.x, y + it.y) })) return false
        iteration++
    }
    return true
}

/**
 * Normalizes offset (converts to linear format)
 */
private fun calculateWalkIndex(diffX: Int, diffY: Int): Int {
    val diffX = diffX + 1
    val diffY = (diffY + 1) shl OFFSET_Y_BITS_COUNT
    var index = diffX or diffY
    return index
}



