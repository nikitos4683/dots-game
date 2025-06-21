package org.dots.game.core

import kotlin.jvm.JvmInline

@JvmInline
value class Position internal constructor(val position: Int) : Comparable<Position> {
    companion object {
        val ZERO = Position(0, 0)
        val GROUND = Position(1, 0)
        val RESIGN = Position(2, 0)
        val STOP = Position(3, 0)
        val TIME = Position(4, 0)
        val INTERRUPT = Position(5, 0)
        val DRAW = Position(6, 0)
        const val COORDINATE_BITS_COUNT = 6
        const val MASK = (1 shl COORDINATE_BITS_COUNT) - 1
    }

    constructor(x: Int, y: Int) : this((x shl COORDINATE_BITS_COUNT) or (y and MASK))

    override fun toString(): String = "($x;$y)"

    val x: Int get() = position shr COORDINATE_BITS_COUNT

    val y: Int get() = position and MASK

    fun isGameOverMove(): Boolean = isGrounding() || isResigning() || this == STOP || this == TIME || this == INTERRUPT || this == DRAW

    fun isGrounding(): Boolean = this == GROUND

    fun isResigning(): Boolean = this == RESIGN

    operator fun component1(): Int = x
    operator fun component2(): Int = y

    override fun compareTo(other: Position): Int {
        return position.compareTo(other.position)
    }
}

infix fun Int.x(that: Int): Position = Position(this, that)