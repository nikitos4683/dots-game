package org.dots.game.field

import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.dots.game.core.EndGameKind
import org.dots.game.core.GameResult
import org.dots.game.core.Player
import org.dots.game.core.Position
import org.dots.game.core.getSortedClosurePositions
import kotlin.test.Test
import kotlin.test.assertEquals

class GroundingTests : FieldTests() {
    @Test
    fun simple() {
        testFieldWithRollback("""
             . . . . .
             . * * + .
             . . . . .
        """) {
            val moveResult = it.makeMove(Position.GROUND, Player.First)!!
            val base = moveResult.bases.single()

            val sortedPositions = base.getSortedClosurePositions(it, isGrounding = true)
            assertEquals(2, sortedPositions.outerClosure.size)
            assertTrue(sortedPositions.innerClosures.isEmpty())

            assertTrue(base.closurePositions.isEmpty())
            assertEquals(0, it.player1Score)
            assertEquals(2, it.player2Score)
            assertEquals(GameResult.ScoreWin(2.0, EndGameKind.Grounding, Player.Second), it.gameResult)
            it.unmakeMove()

            it.makeMove(Position.GROUND, Player.Second)!!
            assertEquals(1, it.player1Score)
            assertEquals(0, it.player2Score)
            assertEquals(GameResult.ScoreWin(1.0, EndGameKind.Grounding, Player.First), it.gameResult)
            it.unmakeMove()
        }
    }

    @Test
    fun grounding() {
        testFieldWithRollback("""
             * +
        """) {
            val moveResult = it.makeMove(Position.GROUND, Player.First)!!
            assertTrue(moveResult.bases.isEmpty())
            assertEquals(0, it.player1Score)
            assertEquals(0, it.player2Score)
            assertEquals(GameResult.Draw(EndGameKind.Grounding), it.gameResult)
            it.unmakeMove()

            it.makeMove(Position.GROUND, Player.Second)
            assertEquals(0, it.player1Score)
            assertEquals(0, it.player2Score)
            assertEquals(GameResult.Draw(EndGameKind.Grounding), it.gameResult)
            it.unmakeMove()
        }
    }

    @Test
    fun base() {
        testFieldWithRollback("""
            . . . . . .
            . . * * . .
            . * + . * .
            . . * * . .
            . . . . . .
        """) {
            val moveResult = it.makeMove(Position.GROUND, Player.First)!!
            val base = moveResult.bases.single()
            val sortedPositions = base.getSortedClosurePositions(it, isGrounding = true)
            assertEquals(6, sortedPositions.outerClosure.size)
            assertTrue(sortedPositions.innerClosures.isEmpty())

            assertEquals(0, it.player1Score)
            assertEquals(6, it.player2Score)
        }
    }

    @Test
    fun multipleGroups() {
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

            val firstBase = moveResult.bases[0]
            val sortedPositions = firstBase.getSortedClosurePositions(it, isGrounding = true)
            assertEquals(1, sortedPositions.outerClosure.size)
            assertTrue(sortedPositions.innerClosures.isEmpty())

            assertEquals(0, it.player1Score)
            assertEquals(2, it.player2Score)
        }
    }

    @Test
    fun invalidateEmptyTerritory() {
        testFieldWithRollback(
            """
            . . . . . .
            . . * * . .
            . * . . * .
            . . * * . .
            . . . . . .
        """
        ) {
            val moveResult = it.makeMove(Position.GROUND, Player.First)!!
            assertEquals(4, moveResult.bases.size)

            with (it) {
                assertFalse(Position(3, 3).getState().checkWithinEmptyTerritory())
                assertFalse(Position(3, 4).getState().checkWithinEmptyTerritory())
            }
        }
    }

    @Test
    fun dontInvalidateEmptyTerritoryForStrongConnection() {
        testFieldWithRollback(
            """
            * * *
            * . *
            * * *
        """
        ) {
            val moveResult = it.makeMove(Position.GROUND, Player.First)!!
            assertTrue(moveResult.bases.isEmpty())

            with (it) {
                assertTrue(Position(2, 2).getState().checkWithinEmptyTerritory(Player.First))
            }
        }
    }
}