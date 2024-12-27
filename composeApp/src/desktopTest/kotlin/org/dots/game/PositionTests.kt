package org.dots.game

import org.dots.game.core.Offset
import org.dots.game.core.Position
import org.dots.game.core.clockwiseWalk
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
    fun testClockwiseWalk() {
        val x = 2
        val y = 2
        var counter = 0

        Position(x, y).clockwiseWalk(Position(x - 1, y - 1)) { nextPosition ->
            val expectedPosition = when (counter) {
                0 -> Position(x, y - 1)
                1 -> Position(x + 1, y - 1)
                2 -> Position(x + 1, y)
                3 -> Position(x + 1, y + 1)
                4 -> Position(x, y + 1)
                5 -> Position(x - 1, y + 1)
                6 -> Position(x - 1, y)
                7 -> Position(x - 1, y - 1)
                else -> assertFails { "Incorrect clockwise walk" }
            }
            assertEquals(expectedPosition, nextPosition)
            counter++
            return@clockwiseWalk true
        }
    }

    @Test
    fun testClockwiseWalk2() {
        val x = 2
        val y = 2
        var counter = 0

        Position(x, y).clockwiseWalk(Position(x - 1, y + 1)) { nextPosition ->
            val expectedPosition = when (counter) {
                0 -> Position(x - 1, y)
                1 -> Position(x - 1, y - 1)
                2 -> Position(x, y - 1)
                3 -> Position(x + 1, y - 1)
                4 -> Position(x + 1, y)
                5 -> Position(x + 1, y + 1)
                6 -> Position(x, y + 1)
                7 -> Position(x - 1, y + 1)
                else -> assertFails { "Incorrect clockwise walk" }
            }
            assertEquals(expectedPosition, nextPosition)
            counter++
            return@clockwiseWalk true
        }
    }
}