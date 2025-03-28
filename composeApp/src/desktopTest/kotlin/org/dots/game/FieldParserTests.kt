package org.dots.game

import org.dots.game.core.MoveResult
import org.dots.game.core.Player
import org.dots.game.core.Position
import org.dots.game.core.x
import org.dots.game.infrastructure.FieldParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FieldParserTests {
    @Test
    fun empty() {
        val parsedField = FieldParser.parseEmptyField("""
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
        val parsedField = FieldParser.parseEmptyField("""
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
        val parsedField = FieldParser.parseEmptyField("""
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
    fun incorrectMarker() {
        assertFailsWith<Exception> { FieldParser.parseEmptyField("x") }
    }

    @Test
    fun incorrectMoveNumber() {
        assertFailsWith<Exception> { FieldParser.parseEmptyField("*-5") }
    }

    @Test
    fun clashingMoveNumbers() {
        val field = """
            *0 +1
            +1 *2
        """
        assertFailsWith<Exception> { FieldParser.parseEmptyField(field) }
    }

    @Test
    fun missingMoveNumbers() {
        val field = """
            *0 +1
            +3 *4
        """
        assertFailsWith<Exception> { FieldParser.parseEmptyField(field) }
    }

    private fun MoveResult.checkPositionAndPlayer(expectedPosition: Position, expectedPlayer: Player) {
        assertEquals(expectedPosition, position)
        assertEquals(expectedPlayer, player)
    }
}