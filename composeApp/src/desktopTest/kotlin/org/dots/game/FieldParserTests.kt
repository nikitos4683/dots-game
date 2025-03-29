package org.dots.game

import org.dots.game.core.MoveResult
import org.dots.game.core.Player
import org.dots.game.core.Position
import org.dots.game.core.x
import org.dots.game.dump.FieldParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class FieldParserTests {
    @Test
    fun empty() {
        val parsedField = FieldParser.parseFieldWithNoInitialMoves("""
            . .
            . .
            . .
        """)

        assertEquals(2, parsedField.width)
        assertEquals(3, parsedField.height)
        assertTrue(parsedField.moveSequence.isEmpty())
    }

    @Test
    fun simple() {
        val parsedField = FieldParser.parseFieldWithNoInitialMoves("""
            . . . .
            . * + .
            . + * .
            . . . .
        """)

        assertEquals(4, parsedField.width)
        assertEquals(4, parsedField.height)
        val moveSequence = parsedField.moveSequence
        moveSequence[0].checkPositionAndPlayer(2 x 2, Player.First)
        moveSequence[1].checkPositionAndPlayer(3 x 2, Player.Second)
        moveSequence[2].checkPositionAndPlayer(2 x 3, Player.Second)
        moveSequence[3].checkPositionAndPlayer(3 x 3, Player.First)
    }

    @Test
    fun simpleWithNumbers() {
        val parsedField = FieldParser.parseFieldWithNoInitialMoves("""
            .  .  . .
            . *0 +3 .
            . +1 *2 .
            .  .  . .
        """)

        val moveSequence = parsedField.moveSequence
        moveSequence[0].checkPositionAndPlayer(2 x 2, Player.First)
        moveSequence[1].checkPositionAndPlayer(2 x 3, Player.Second)
        moveSequence[2].checkPositionAndPlayer(3 x 3, Player.First)
        moveSequence[3].checkPositionAndPlayer(3 x 2, Player.Second)
    }

    @Test
    fun mixedNumberedAndUnnumberedMoves() {
        val parsedField = FieldParser.parseFieldWithNoInitialMoves("""
            . *0 .
            * +2 *
            . * .
        """)

        val moveSequence = parsedField.moveSequence
        moveSequence[0].checkPositionAndPlayer(2 x 1, Player.First)
        moveSequence[1].checkPositionAndPlayer(1 x 2, Player.First)
        moveSequence[2].checkPositionAndPlayer(2 x 2, Player.Second)
        moveSequence[3].checkPositionAndPlayer(3 x 2, Player.First)
        moveSequence[4].checkPositionAndPlayer(2 x 3, Player.First)
    }

    @Test
    fun lastNumbered() {
        val parsedField = FieldParser.parseFieldWithNoInitialMoves("""
            * +
            + *3
        """)
        val moveSequence = parsedField.moveSequence
        assertEquals(4, moveSequence.size)
        moveSequence[3].checkPositionAndPlayer(2 x 2, Player.First)
    }

    @Test
    fun incorrectMarker() {
        assertEquals(
            "Error at [0..1): The marker should be either `*` (first player), `+` (second player) or `.`.",
        assertFails { FieldParser.parseFieldWithNoInitialMoves("x") }.message
        )
    }

    @Test
    fun incorrectMoveNumber() {
        assertEquals(
            "Error at [1..13): Incorrect cell move's number.",
        assertFails { FieldParser.parseFieldWithNoInitialMoves("*999999999999") }.message
        )
    }

    @Test
    fun clashingMoveNumbers() {
        val field = """
            *0 +1
            +1 *2
        """
        assertEquals(
            "Error at [32..33): The move with number 1 is already in use.",
            assertFails { FieldParser.parseFieldWithNoInitialMoves(field) }.message
        )
    }

    @Test
    fun missingMoveNumbers() {
        val field = """
            *0 +1
            +4 *5
        """
        assertEquals(
            "Error: The following moves are missing: 2..3",
            assertFails { FieldParser.parseFieldWithNoInitialMoves(field) }.message
        )
    }

    private fun MoveResult.checkPositionAndPlayer(expectedPosition: Position, expectedPlayer: Player) {
        assertEquals(expectedPosition, position)
        assertEquals(expectedPlayer, player)
    }
}