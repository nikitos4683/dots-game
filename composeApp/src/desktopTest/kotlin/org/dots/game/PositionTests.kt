package org.dots.game

import org.dots.game.core.Offset
import org.dots.game.core.Position
import org.dots.game.core.clockwiseBigJumpWalk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class PositionTests {
    @Test
    fun testPosition() {
        assertEquals("(0;0)", Position(0, 0).toString())
        assertEquals("(5;5)", Position(5, 5).toString())
    }

    @Test
    fun testOffset() {
        assertEquals("(0;0)", Offset(0, 0).toString())
        assertEquals("(1;1)", Offset(1, 1).toString())
        assertEquals("(-1;1)", Offset(-1, 1).toString())
        assertEquals("(1;-1)", Offset(1, -1).toString())
        assertEquals("(-1;-1)", Offset(-1, -1).toString())
        assertEquals("(-10;-10)", Offset(-10, -10).toString())
    }

    @Test
    fun testClockwiseBigJumpWalk() {
        val x = 2
        val y = 2
        val position = Position(2, 2)
        var counter = 0

        val positionsToCheck = listOf(
            Position(x - 1, y - 1),
            Position(x, y - 1),
            Position(x + 1, y - 1),
            Position(x + 1, y),
            Position(x + 1, y + 1),
            Position(x, y + 1),
            Position(x - 1, y + 1),
            Position(x - 1, y),
        )

        for (positionToCheck in positionsToCheck) {
            position.clockwiseBigJumpWalk(positionToCheck) {
                val expectedPosition = when (counter) {
                    0 -> Position(x + 1, y - 1)
                    1 -> Position(x + 1, y + 1)
                    2 -> Position(x + 1, y + 1)
                    3 -> Position(x - 1, y + 1)
                    4 -> Position(x - 1, y + 1)
                    5 -> Position(x - 1, y - 1)
                    6 -> Position(x - 1, y - 1)
                    7 -> Position(x + 1, y - 1)
                    else -> assertFails { "Incorrect clockwiseBigJump walk" }
                }

                assertEquals(expectedPosition, it)

                counter++
                return@clockwiseBigJumpWalk false
            }
        }
    }
}