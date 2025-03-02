package org.dots.game.field

import org.dots.game.core.Player
import org.dots.game.core.x
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FieldWithEmptyCapturing() : FieldTests() {
    override val captureEmptyBase: Boolean = true

    @Test
    fun checkCapturing() {
        testFieldWithRollback("""
            . * .
            * . *
            . * .
        """) {
            assertEquals(0, it.player1Score)
            assertNull(it.makeMove(2 x 2, Player.First))
            assertNull(it.makeMove(2 x 2, Player.Second))
        }
    }
}