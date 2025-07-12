package org.dots.game.field

import org.dots.game.core.DotState
import org.dots.game.core.EndGameKind
import org.dots.game.core.GameResult
import org.dots.game.core.Player
import org.dots.game.core.Position
import org.dots.game.core.x
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class StandardFieldTests : FieldTests() {
    @Test
    fun testInvalidMoveOnThePlacedDot() {
        testFieldWithRollback("""
            . . .
            . * .
            . . .
        """) {
            assertNull(it.makeMove(2 x 2, Player.First))
        }
    }

    @Test
    fun checkCapturing() {
        testFieldWithRollback("""
            . * .
            * + *
            . . .
        """) {
            val moveResult = it.makeMove(2 x 3, Player.First)!!
            val base = moveResult.bases.single()
            assertEquals(
                listOf(Position(2, 3), Position(3, 2), Position(2, 1), Position(1, 2)),
                base.closurePositions
            )
            assertEquals(Position(2, 2), base.previousPositionStates.single().position)
        }
    }

    @Test
    fun simpleCapture() {
        testFieldWithRollback("""
            . * .
            * + *
            . * .
        """) {
            assertEquals(1, it.player1Score)
            assertEquals(0, it.player2Score)
        }
    }

    @Test
    fun simpleCapture2() {
        testFieldWithRollback("""
            .  *2 .
            *0 +1 *3
            .  *5 *4
        """) {
            assertEquals(1, it.player1Score)
        }
    }

    @Test
    fun simpleCaptureForOppositePlayer() {
        testFieldWithRollback("""
            . + .
            + * +
            . + .
        """) {
            assertEquals(0, it.player1Score)
            assertEquals(1, it.player2Score)
        }
    }

    @Test
    fun tripleCapture() {
        testFieldWithRollback("""
            . * . * .
            * + . + *
            . * + * .
            . . * . .
        """) {
            val moveResult = it.makeMove(3 x 2, Player.First)!!
            val bases = moveResult.bases
            assertEquals(3, bases.size)
            assertEquals(3, it.player1Score)
        }
    }

    @Test
    fun invalidMoveWithinBase(){
        testFieldWithRollback("""
            . * * .
            * + . *
            . * * .
        """) {
            assertNull(it.makeMove(3 x 2, Player.First))
            assertNull(it.makeMove(3 x 2, Player.Second))
        }
    }

    @Test
    fun capturedDotIsNotActive() {
        testFieldWithRollback("""
            . * + .
            * + * +7
            . * + .
        """) {
            assertEquals(1, it.player1Score)
            assertEquals(0, it.player2Score)
        }
    }

    @Test
    fun emptyBase() {
        testFieldWithRollback("""
            . *  .
            * +4 *
            . *  .
        """) {
            assertEquals(1, it.player1Score)
        }
    }

    @Test
    fun outerEmptyBaseSurroundsNonEmptyBase() {
        testFieldWithRollback(
            """
            ... ... +04 +05 +06
            ... +07 ... ... ... +08
            +09 ... ... +00 ... ... +10
            +11 ... +01 *20 +02 ... +12
            +13 ... ... +03 ... ... +14
            ... +15 ... ... ... +16
            ... ... +17 +18 +19
        """
        ) {
            assertEquals(1, it.player2Score)

            assertNotNull(it.makeMove(6 x 4, Player.Second))
            assertNull(it.makeMove(6 x 4, Player.Second))

            assertNotNull(it.makeMove(4 x 2, Player.First))
            assertEquals(2, it.player2Score)
        }
    }

    @Test
    fun newNonEmptyBaseWithinOuterEmptyBase() {
        testFieldWithRollback("""
            ... ... +00 +01 +02
            ... +03 ... ... ... +04
            +05 ... ... +16 ... ... +06
            +07 ... +17 *20 +18 ... +08
            +09 ... ... +19 ... ... +10
            ... +11 ... ... ... +12
            ... ... +13 +14 +15
        """) {
            assertEquals(1, it.player2Score)

            assertNotNull(it.makeMove(4 x 2, Player.First))
            assertEquals(2, it.player2Score)
        }
    }

    @Test
    fun emptyBaseWithinEmptyBase3() {
        testFieldWithRollback("""
            ... ... +20 +05 +06
            ... +07 ... ... ... +08
            +09 ... ... +00 ... ... +10
            +11 ... +01 *04 +02 ... +12
            +13 ... ... +03 ... ... +14
            ... +15 ... ... ... +16
            ... ... +17 +18 +19
        """) {
            assertEquals(1, it.player2Score)

            assertNotNull(it.makeMove(4 x 2, Player.First))
            assertEquals(2, it.player2Score)
        }
    }

    @Test
    fun emptyBaseWithinEmptyBase4() {
        testFieldWithRollback("""
            ... ... +20 +05 +06
            ... +07 ... ... ... +08 +21 +22
            +09 ... ... +00 ... ... ... ... +10
            +11 ... +01 *04 +02 ... *25 ... +12
            +13 ... ... +03 ... ... ... ... +14
            ... +15 ... ... ... +16 +23 +24
            ... ... +17 +18 +19
        """) {
            assertEquals(2, it.player2Score)
            assertNull(it.makeMove(8 x 4, Player.First))
        }
    }

    @Test
    fun emptyBaseWithinEmptyBase5() {
        testFieldWithRollback("""
            ... ... +04 +05 +06
            ... +07 ... ... ... +08 +20 +21
            +09 ... ... +00 ... ... ... ... +10
            +11 ... +01 ... +02 ... *24 ... +12
            +13 ... ... +03 ... ... ... ... +14
            ... +15 ... ... ... +16 +22 +23
            ... ... +17 +18 +19
        """) {
            assertEquals(1, it.player2Score)
            assertNull(it.makeMove(8 x 4, Player.First))
            assertNull(it.makeMove(4 x 4, Player.First))
        }
    }

    @Test
    fun emptyBaseWithinEmptyBase6() {
        testFieldWithRollback("""
            ... ... ... +04 +05 +06 +07 +08 ... ... ...
            ... ... +31 ... ... ... ... ... +09 ... ...
            ... +30 ... ... +32 +33 +34 ... ... +10 ...
            +29 ... ... +47 ... ... ... +35 ... ... +11
            +28 ... +46 ... ... +00 ... ... +36 ... +12
            +27 ... +45 ... +03 ... +01 ... +37 ... +13
            +26 ... +44 ... ... +02 ... ... +38 ... +14
            +25 ... ... +43 ... ... ... +39 ... ... +15
            ... +24 ... ... +42 +41 +40 ... ... +16 ...
            ... ... +23 ... ... ... ... ... +17 ... ...
            ... ... ... +22 +21 +20 +19 +18 ... ... ...
        """) {
            assertNotNull(it.makeMove(6 x 6, Player.First))
            assertEquals(1, it.player2Score)
            assertNotNull(it.makeMove(4 x 6, Player.First))
            assertEquals(2, it.player2Score)
            assertNotNull(it.makeMove(2 x 6, Player.First))
            assertEquals(3, it.player2Score)

            assertNotNull(it.unmakeMove())
            assertNotNull(it.unmakeMove())
            assertNotNull(it.unmakeMove())

            assertNotNull(it.makeMove(4 x 6, Player.First))
            assertEquals(1, it.player2Score)
            assertNull(it.makeMove(6 x 6, Player.First))
            assertNotNull(it.makeMove(2 x 6, Player.First))
            assertEquals(2, it.player2Score)

            assertNotNull(it.unmakeMove())
            assertNotNull(it.unmakeMove())

            assertNotNull(it.makeMove(2 x 6, Player.First))
            assertEquals(1, it.player2Score)
            assertNull(it.makeMove(6 x 6, Player.First))
            assertNull(it.makeMove(4 x 6, Player.First))
            assertNotNull(it.unmakeMove())
        }
    }

    @Test
    fun preventEndlessLoopOnCaptureChecking() {
        testFieldWithRollback("""
            . + +   + +
            + . .   . . +
            + . +14 . . *15 +
            + . .   . . +
            . + +   + +
        """) {
            assertEquals(1, it.player2Score)
        }
    }

    @Test
    fun player1CapturesByPlacingInsideEmptyBase() {
        testFieldWithRollback("""
            . *  +  .
            * +6 *7 +
            . *  +  .
        """) {
            assertEquals(1, it.player1Score)
            assertEquals(0, it.player2Score)
        }
    }

    @Test
    fun baseInsideBase() {
        testFieldWithRollback("""
            .. ..  * .. ..
            ..  *  +1 * ..
             *  +2 *0 +3 * 
            ..  *  +4 * ..
            .. ..  * .. ..
        """) {
            assertEquals(4, it.player1Score)
            assertEquals(0, it.player2Score)
        }
    }

    @Test
    fun baseInsideBaseInsideBase() {
        testFieldWithRollback("""
            .. ..  ..  +
            .. ..  +   *05 +
            .. +   *06 +01 *07 +
             + *08 +02 *00 +03 *09 +
            .. +   *10 +04 *11 +
            .. ..  +   *12 +
            .. .. ..   +
        """) {
            assertEquals(0, it.player1Score)
            assertEquals(9, it.player2Score)
        }
    }

    @Test
    fun enemyEmptyBaseInsideBase() {
        testFieldWithRollback("""
            ..   ..  *5  .. ..
            ..   *4  +0 *6  ..
             *11 +3  .. +1 *7
            ..   *10 +2 *8  ..
            ..   ..  *9  .. ..
        """) {
            assertEquals(4, it.player1Score)
            assertNull(it.makeMove(3 x 3, Player.First))
            assertNull(it.makeMove(3 x 3, Player.Second))
        }
    }

    @Test
    fun enemyEmptyBasesInsideBase() {
        testFieldWithRollback("""
            ... ... ... *45 *46 *20 *21 *22 ... ... ...
            ... ... ... ... ... ... ... ... *23 ... ...
            ... *44 ... ... +19 +04 +05 ... ... *24 ...
            *43 ... ... +18 ... ... ... +06 ... ... *25
            *42 ... +17 ... ... +00 ... ... +07 ... *26
            *41 ... +16 ... +03 ... +01 ... +08 ... *27
            *40 ... +15 ... ... +02 ... ... +09 ... *28
            *39 ... ... +14 ... ... ... +10 ... ... *29
            ... *38 ... ... +13 +12 +11 ... ... *30 ...
            ... ... *37 ... ... ... ... ... *31 ... ...
            ... ... ... *36 *35 *34 *33 *32 ... ... ...
        """) {
            assertNotNull(it.makeMove(3 x 2, Player.First))
            assertNull(it.makeMove(6 x 6, Player.First))
            assertNull(it.makeMove(6 x 6, Player.Second))
            assertNull(it.makeMove(4 x 6, Player.First))
            assertNull(it.makeMove(2 x 6, Player.First))
            assertEquals(20, it.player1Score)
            assertNotNull(it.unmakeMove())

            assertNotNull(it.makeMove(6 x 6, Player.First))
            assertEquals(1, it.player2Score)
            assertNotNull(it.makeMove(3 x 2, Player.First))
            assertEquals(20, it.player1Score)
            assertEquals(0, it.player2Score)
            assertNull(it.makeMove(4 x 6, Player.First))
            assertNull(it.makeMove(2 x 6, Player.First))
            assertNotNull(it.unmakeMove())
        }
    }

    @Test
    fun checkTopEdge() {
        testFieldWithRollback("""
            .  *0 .
            *3 +  *1
            .  *2 .
        """) {
            assertEquals(1, it.player1Score)
        }
    }

    @Test
    fun checkRightEdge() {
        testFieldWithRollback("""
            .  *3 .
            *2 +  *0
            .  *1 .
        """) {
            assertEquals(1, it.player1Score)
        }
    }

    @Test
    fun checkBottomEdge() {
        testFieldWithRollback("""
            .  *2 .
            *1 + *3
            .  *0 .
        """) {
            assertEquals(1, it.player1Score)
        }

    }

    @Test
    fun checkLeftEdge() {
        testFieldWithRollback("""
            .  *1 .
            *0 + *2
            .  *3 .
        """) {
            assertEquals(1, it.player1Score)
        }
    }

    @Test
    fun dontProceedWithWalkIfEncounterABorder() {
        testFieldWithRollback("""
            .  .  *1 .
            *0 *5 +4 *2
            .  .  *3 .
        """) {
            assertEquals(1, it.player1Score)
        }
    }

    @Test
    fun invalidateEmptyTerritoryWhenItsBorderCaptured() {
        testFieldWithRollback("""
            .  +0 +1 *6
            +5 .  *9 +2 *7
            .  +4 +3 *8
        """) {
            assertEquals(DotState.Empty, with (it) { Position(2, 2).getState() })
        }
    }

    @Test
    fun invalidateEmptyTerritoryWhenItsBorderCaptured2() {
        testFieldWithRollback(
            """
            ... ... +04 +05 +06
            ... +07 ... ... ... +08
            +09 ... ... +00 ... ... +10 *20
            +11 ... +01 ... +02 ... ... +12 *21
            +13 ... ... +03 ... ... +14 *22
            ... +15 ... ... ... +16
            ... ... +17 +18 +19
        """
        ) {
            assertNotNull(it.makeMove(7 x 4, Player.First))
            assertEquals(1, it.player1Score)
            assertEquals(0, it.player2Score)

            assertNotNull(it.makeMove(2 x 4, Player.First))
            assertEquals(1, it.player1Score)
            assertEquals(0, it.player2Score)

            assertNotNull(it.makeMove(4 x 4, Player.First))
            assertEquals(1, it.player1Score)
            assertEquals(1, it.player2Score)
        }
    }

    @Test
    fun tryPlacingToTerritoryThatBecameCapturedAfterBeingEmpty() {
        testFieldWithRollback(
            """
            .  *1 *2 .
            *0 +6 .  *3
            .  *5 *4 .
        """
        ) {
            assertEquals(1, it.player1Score)
            assertNull(it.makeMove(3 x 2, Player.First))
        }
    }

    @Test
    fun capturingAfterPlacingToEmptyTerritoryShouldBeMinimal() {
        testFieldWithRollback("""
            .  .  .  .  .
            .  .  *2 .  .
            .  *1 .  *3 .
            .  *6 *7 *4 .
            .  .  *5 .  .
            .  .  .  .  .
        """) {
            val moveResult = it.makeMove(3 x 3, Player.Second)!!
            val base = moveResult.bases.single()
            assertEquals(1, base.previousPositionStates.size)
        }
    }

    @Test
    fun complexEmptyBase() {
        testFieldWithRollback("""
            .   .   *2  *3  .
            .   *12 .   .   *4
            *11 .   *1  .   *5
            *10 .   .   .   *6
            .   *9  *8  *7  .
        """) {
            it.makeMove(3 x 2, Player.Second)!!
        }
    }

    @Test
    fun singularConnectionForEmptyBase() {
        testFieldWithRollback("""
            .  *3 *6 .
            *2 *1 .  *7
            .  *4 *5 .
        """) {
            it.makeMove(3 x 2, Player.Second)!!
        }
    }

    @Test
    fun gameFinishedWithNoLegalMoves() {
        testFieldWithRollback("""
            * * *
            * * *
            * * *
        """.trimIndent()) {
            assertEquals(EndGameKind.NoLegalMoves, (it.gameResult as GameResult.Draw).endGameKind)
        }
    }

    @Test
    fun gameFinishedWithNoLegalMovesAndBase() {
        testFieldWithRollback("""
            * * * *
            * + . *
            * * * *
        """.trimIndent()) {
            val gameResult = it.gameResult as GameResult.ScoreWin
            assertEquals(EndGameKind.NoLegalMoves, gameResult.endGameKind)
            assertEquals(1.0, gameResult.score)
            assertEquals(Player.First, gameResult.winner)
        }
    }

    @Test
    fun noGameFinishedIfSuicidalMoveToEmptyTerritoryRemains() {
        testFieldWithRollback("""
            * * * *
            * . . *
            * * * *
        """.trimIndent()) {
            assertNull(it.gameResult)
            it.makeMove(2 x 2, Player.Second)
            val gameResult = it.gameResult as GameResult.ScoreWin
            assertEquals(EndGameKind.NoLegalMoves, gameResult.endGameKind)
            assertEquals(1.0, gameResult.score)
            assertEquals(Player.First, gameResult.winner)
        }
    }

    @Test
    fun gameFinishedWithNoLegalMovesAndBaseInsideBase() {
        testFieldWithRollback("""
            * * * * * *
            * . + + . *
            * + * . + *
            * . + + . *
            * * * * * *
        """.trimIndent()) {
            val gameResult = it.gameResult as GameResult.ScoreWin
            assertEquals(EndGameKind.NoLegalMoves, gameResult.endGameKind)
            assertEquals(6.0, gameResult.score)
            assertEquals(Player.First, gameResult.winner)
        }
    }
}
