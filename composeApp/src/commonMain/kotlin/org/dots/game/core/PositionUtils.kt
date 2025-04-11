package org.dots.game.core

private const val OFFSET_X_BITS_COUNT = 2
private const val OFFSET_Y_BITS_COUNT = 2

private const val X_MINUS_ONE = -1
private const val X = 0
private const val X_PLUS_ONE = +1
private const val Y_MINUS_ONE = -1
private const val Y = 0
private const val Y_PLUS_ONE = +1

private const val LEFT_TOP_INDEX = (X_MINUS_ONE + 1) or ((Y_MINUS_ONE + 1) shl OFFSET_Y_BITS_COUNT)
private const val TOP_INDEX = (X + 1) or ((Y_MINUS_ONE + 1) shl OFFSET_Y_BITS_COUNT)
private const val RIGHT_TOP_INDEX = (X_PLUS_ONE + 1) or ((Y_MINUS_ONE + 1) shl OFFSET_Y_BITS_COUNT)
private const val RIGHT_INDEX = (X_PLUS_ONE + 1) or ((Y + 1) shl OFFSET_Y_BITS_COUNT)
private const val RIGHT_BOTTOM_INDEX = (X_PLUS_ONE + 1) or ((Y_PLUS_ONE + 1) shl OFFSET_Y_BITS_COUNT)
private const val BOTTOM_INDEX = (X + 1) or ((Y_PLUS_ONE + 1) shl OFFSET_Y_BITS_COUNT)
private const val LEFT_BOTTOM_INDEX = (X_MINUS_ONE + 1) or ((Y_PLUS_ONE + 1) shl OFFSET_Y_BITS_COUNT)
private const val LEFT_INDEX = (X_MINUS_ONE + 1) or ((Y + 1) shl OFFSET_Y_BITS_COUNT)

private val nextBigJumpPositionIndex: IntArray = IntArray(1 shl (OFFSET_X_BITS_COUNT + OFFSET_Y_BITS_COUNT)) { -1 }

private val positionOffset: Array<Offset> = buildList {
    var positionOffsetIndex = 0

    fun calculate(diffX: Int, diffY: Int) {
        var index = calculateWalkIndex(diffX, diffY)
        nextBigJumpPositionIndex[index] = (positionOffsetIndex + 2 + positionOffsetIndex % 2) % 8
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

fun Position.squareDistanceTo(other: Position): Int {
    val diffX = x - other.x
    val diffY = y - other.y
    return diffX * diffX + diffY * diffY
}

fun Position.squareDistanceToZero(): Int {
    return x * x + y * y
}

fun Position.getSquare(other: Position): Int {
    return y * other.x - other.y * x
}

internal fun Position.getNextClockwisePosition(other: Position): Position {
    return when (val walkIndex = calculateWalkIndex(other.x - x, other.y - y)) {
        LEFT_TOP_INDEX -> Position(x, y - 1)
        TOP_INDEX -> Position(x + 1, y - 1)
        RIGHT_TOP_INDEX -> Position(x + 1, y)
        RIGHT_INDEX -> Position(x + 1, y + 1)
        RIGHT_BOTTOM_INDEX -> Position(x, y + 1)
        BOTTOM_INDEX -> Position(x - 1, y + 1)
        LEFT_BOTTOM_INDEX -> Position(x - 1, y)
        LEFT_INDEX -> Position(x - 1, y - 1)
        else -> error("Incorrect walk index $walkIndex")
    }
}

/**
 * Call an @param[action] on each position during walking over positions that surrounds @param[Position].
 * The first position to walk relative the passed @param[other] is calculated in the following way:
 *
 * ```
 *   x . *   . x .   . . x   . . .   . . .   * . .   * . .   . . *
 *   . o .   . o .   . o .   . o x   . o .   . o .   . o .   x o .
 *   . . .   . . *   . . *   * . .   * . x   . x .   x . .   . . .
 * ```
 *  - o @param[Position]
 *  - x @param[other]
 *  - * first position to consider.
 *
 *  @return `true` if no predicate matches (typically, it's a single dot not linked to any other).
 */
internal inline fun Position.clockwiseBigJumpWalk(other: Position, action: (Position) -> Boolean): Boolean {
    val (x, y) = this
    val initialIndex = nextBigJumpPositionIndex[calculateWalkIndex(other.x - x, other.y - y)]
    val initialIndexAfterLoop = initialIndex + 8
    var index = initialIndex

    do {
        if (!action(positionOffset[index++].let { Position(x + it.x, y + it.y) })) return false
    } while (index != initialIndexAfterLoop)

    return true
}

/**
 * Normalizes offset (converts to linear format)
 */
private fun calculateWalkIndex(diffX: Int, diffY: Int): Int = (diffX + 1) or ((diffY + 1) shl OFFSET_Y_BITS_COUNT)



