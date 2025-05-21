package org.dots.game.field

import junit.framework.TestCase.assertTrue
import org.dots.game.core.Player
import org.dots.game.core.Position
import kotlin.test.Test
import kotlin.test.assertEquals

class GroundingTests : FieldTests() {
    @Test
    fun testSimple() {
        testFieldWithRollback("""
             . . . .
             . * + .
             . . . .
        """) {
            val moveResult = it.makeMove(Position.GROUND, Player.First)!!
            val base = moveResult.bases.single()
            assertTrue(base.closurePositions.isEmpty())
            assertEquals(0, it.player1Score)
            assertEquals(1, it.player2Score)
            it.unmakeMove()

            it.makeMove(Position.GROUND, Player.Second)!!
            assertEquals(1, it.player1Score)
            assertEquals(0, it.player2Score)
            it.unmakeMove()
        }
    }

    @Test
    fun testGrounding() {
        testFieldWithRollback("""
             * +
        """) {
            val moveResult = it.makeMove(Position.GROUND, Player.First)!!
            assertTrue(moveResult.bases.isEmpty())
            assertEquals(0, it.player1Score)
            assertEquals(0, it.player2Score)
            it.unmakeMove()

            it.makeMove(Position.GROUND, Player.Second)
            assertEquals(0, it.player1Score)
            assertEquals(0, it.player2Score)
            it.unmakeMove()
        }
    }

    @Test
    fun testBase() {
        testFieldWithRollback("""
            . . . . . .
            . . * * . .
            . * + . * .
            . . * * . .
            . . . . . .
        """) {
            val moveResult = it.makeMove(Position.GROUND, Player.First)!!
            assertEquals(1, moveResult.bases.size)

            assertEquals(0, it.player1Score)
            assertEquals(6, it.player2Score)
        }
    }

    @Test
    fun testMultipleGroups() {
        testFieldWithRollback(
            """
            . . . .
            . * + .
            . + * .
            . . . .
        """
        ) {
            val moveResult = it.makeMove(Position.GROUND, Player.First)!!
            assertEquals(2, moveResult.bases.size)

            assertEquals(0, it.player1Score)
            assertEquals(2, it.player2Score)
        }
    }
}