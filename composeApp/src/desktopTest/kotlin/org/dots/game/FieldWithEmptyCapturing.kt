package org.dots.game

import org.dots.game.core.Player
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FieldWithEmptyCapturing() : FieldTests() {
    override val captureEmptyBase: Boolean = true

    @Test
    fun checkCapturing() {
        val field = initialize("""
            . * .
            * . *
            . * .
        """)

        assertEquals(0, field.player1Score)
        assertNull(field.makeMove(1, 1, Player.First))
        assertNull(field.makeMove(1, 1, Player.Second))
    }
}