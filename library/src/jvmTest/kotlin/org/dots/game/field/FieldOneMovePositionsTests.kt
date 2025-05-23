package org.dots.game.field

import org.dots.game.core.Player
import org.dots.game.core.Position
import org.dots.game.core.getOneMoveCapturingAndBasePositions
import org.dots.game.dump.FieldParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FieldOneMovePositionsTests {
    @Test
    fun twoCapturing() {
        val field = FieldParser.parseAndConvertWithNoInitialMoves("""
            . * . . . + .
            * + * . + * +
            . . . . . . .
        """.trimIndent())

        val (capturingPositions, basePositions) = field.getOneMoveCapturingAndBasePositions()

        assertEffectivelyEmpty(basePositions)

        val player1Position = capturingPositions.getValue(Player.First).single()
        val player2Position = capturingPositions.getValue(Player.Second).single()

        assertEquals(Position(2, 3), player1Position)
        assertEquals(Position(6, 3), player2Position)
    }

    @Test
    fun twoBases() {
        val field = FieldParser.parseAndConvertWithNoInitialMoves("""
            . * . . . + .
            * . * . + . +
            . . . . . . .
        """.trimIndent())

        val (capturingPositions, basePositions) = field.getOneMoveCapturingAndBasePositions()

        assertEffectivelyEmpty(capturingPositions)

        val player1Position = basePositions.getValue(Player.First).single()
        val player2Position = basePositions.getValue(Player.Second).single()

        assertEquals(Position(2, 2), player1Position)
        assertEquals(Position(6, 2), player2Position)
    }

    @Test
    fun emptyBasePosition() {
        val field = FieldParser.parseAndConvertWithNoInitialMoves("""
            . * .
            * . *
            . * .
        """.trimIndent())

        val (capturingPositions, basePositions) = field.getOneMoveCapturingAndBasePositions()

        assertEffectivelyEmpty(capturingPositions)

        val basePosition = basePositions.getValue(Player.First).single()
        assertEquals(Position(2, 2), basePosition)
        assertTrue(basePositions.getValue(Player.Second).isEmpty())
    }

    @Test
    fun noBaseIfCapturing() {
        val field = FieldParser.parseAndConvertWithNoInitialMoves("""
            . * .
            * + *
            + . +
            . + .
        """.trimIndent())

        val (capturingPositions, basePositions) = field.getOneMoveCapturingAndBasePositions()

        val player1Capturing = capturingPositions.getValue(Player.First).single()
        assertEquals(Position(2, 3), player1Capturing)
        assertTrue(capturingPositions.getValue(Player.Second).isEmpty())

        assertEffectivelyEmpty(basePositions)
    }

    @Test
    fun twoCapturingOnTheSamePosition() {
        val field = FieldParser.parseAndConvertWithNoInitialMoves("""
            . * .
            * + *
            . . .
            + * +
            . + .
        """.trimIndent())

        val (capturingPositions, basePositions) = field.getOneMoveCapturingAndBasePositions()

        val player1Position = capturingPositions.getValue(Player.First).single()
        val player2Position = capturingPositions.getValue(Player.Second).single()
        assertEquals(Position(2, 3), player1Position)
        assertEquals(player1Position, player2Position)

        assertEffectivelyEmpty(basePositions)
    }

    @Test
    fun twoBasesOnTheSamePosition() {
        // Probably it makes sense to filter out inner capturing positions
        val field = FieldParser.parseAndConvertWithNoInitialMoves(
            """
            . * * * .
            * . + . *
            * + . + *
            * . . . * 
            . * . * .
        """.trimIndent()
        )

        val (capturingPositions, basePositions) = field.getOneMoveCapturingAndBasePositions()

        val player1Capturing = capturingPositions.getValue(Player.First)
        assertEquals(setOf(Position(3, 4), Position(3, 5)), player1Capturing)

        // Capturing positions always overlap base positions
        assertTrue(capturingPositions.getValue(Player.Second).isEmpty())

        val player1Base = basePositions.getValue(Player.First)
        assertEquals(
            setOf(Position(4, 4), Position(4, 2), Position(3, 3), Position(2, 2), Position(2, 4)),
            player1Base
        )
    }

    private fun assertEffectivelyEmpty(positions: Map<Player, Set<Position>>) {
        assertTrue(positions.getValue(Player.First).isEmpty())
        assertTrue(positions.getValue(Player.Second).isEmpty())
    }
}