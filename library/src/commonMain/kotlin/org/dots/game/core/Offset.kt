package org.dots.game.core

import kotlin.jvm.JvmInline

@JvmInline
value class Offset private constructor(val offset: Int) {
    companion object {
        private const val BITS_COUNT = 8
        private const val MASK = (1 shl BITS_COUNT) - 1
        private const val SIGN_BIT = 1 shl (BITS_COUNT - 1)
        private const val SIGN_COMPLEMENT = MASK.inv()
    }

    constructor(x: Int, y: Int) : this((x shl BITS_COUNT) or (y and MASK))

    override fun toString(): String = "($x;$y)"

    val x: Int get() = offset shr BITS_COUNT

    val y: Int get() = (offset and MASK).let { if (it and SIGN_BIT == 0) it else it or SIGN_COMPLEMENT }

    operator fun component1(): Int = x
    operator fun component2(): Int = y
}