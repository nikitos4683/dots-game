package org.dots.game.field

import org.dots.game.core.BaseMode
import org.dots.game.core.Player
import org.dots.game.core.getSortedClosurePositions
import org.dots.game.core.x
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FieldWithAllOpponentDotsAndBorderTests : FieldTests() {
    override val baseMode: BaseMode = BaseMode.AllOpponentDots
    override val captureByBorder: Boolean = true
    override val suicideAllowed: Boolean = false

    @Test
    fun simple(){
        testFieldWithRollback("""
            + * .
            . * .
            * . .
            . . .
        """) {
            assertEquals(0, it.player1Score)
            assertEquals(0, it.player2Score)

            assertNull(it.makeMove(1 x 2, Player.Second)) // Suicide is disallowed

            val moveResult = it.makeMove(1 x 2, Player.First)!!
            val base = moveResult.bases.single()
            val (outerClosure, innerClosure) = base.getSortedClosurePositions(it)

            assertEquals(listOf(1 x 0, 0 x 1, 1 x 2, 2 x 1), outerClosure)
            assertTrue(innerClosure.isEmpty())
            assertEquals(1, it.player1Score)
            assertEquals(0, it.player2Score)

            it.makeMove(3 x 4, Player.Second)
            it.makeMove(2 x 4, Player.First)
            val base2 = it.makeMove(3 x 3, Player.First)!!.bases.single()

            val (outerClosure2, innerClosure2) = base2.getSortedClosurePositions(it)
            assertEquals(listOf(3 x 3, 2 x 4, 3 x 5, 4 x 4), outerClosure2)
            assertTrue(innerClosure2.isEmpty())
        }
    }

    @Test
    fun prohibitMoveIfResultGroupHasNoLiberties() {
        testFieldWithRollback("""
            * *
            * .
        """) {
            // Follow Go game in this case: the result group should have at least one liberty
            assertNull(it.makeMove(2 x 2, Player.First))
        }
    }

    @Test
    fun tryPutDotToEmptyBaseAndCapture() {
        testFieldWithRollback(
            """
            *5 +0 *2 ..
            +1 *3 .. ..
            *4 .. .. ..
            .. .. .. ..
        """
        ) {
            assertEquals(2, it.player1Score)
        }
    }

    @Test
    fun baseWithInternalCornerHole() {
        testFieldWithRollback(
            """
            ... +00 *02 +07 ...
            +01 *04 *03 +08 ...
            *06 *05 +09 ... ...
            +11 +10 ... ... ...
            ... ... ... ... ...
        """
        ) {
            assertEquals(5, it.player2Score)
            assertNull(it.makeMove(1 x 1, Player.First))
            assertNotNull(it.makeMove(1 x 1, Player.Second))
        }
    }

    @Test
    fun dontRecaptureAlreadyCaptured() {
        testFieldWithRollback(
            """
            +00 *01 +03 *08 ...
            *02 +05 +04 *09 ...
            +07 +06 *10 ... ...
            ... *11 ... ... ...
            ... ... ... ... ...
        """
        ) {
            assertEquals(0, it.player1Score)
            assertEquals(2, it.player2Score)

            assertNotNull(it.makeMoveUnsafe(1 x 4, Player.First))

            assertEquals(6, it.player1Score)
            assertEquals(0, it.player2Score)
        }
    }
}

class FieldWithAllOpponentDotsAndNoBorderTests : FieldTests() {
    override val baseMode: BaseMode = BaseMode.AllOpponentDots
    override val captureByBorder: Boolean = false
    override val suicideAllowed: Boolean = false

    @Test
    fun simple() {
        testFieldWithRollback("""
            + * .
            * . .
        """) {
            assertEquals(0, it.player1Score)
            assertEquals(0, it.player2Score)
        }
    }

    @Test
    fun quadrupleCapture() {
        testFieldWithRollback("""
            . . *0 . .
            . *7 + *1 .
            *6 + . + *2
            . *5 + *3 .
            . . *4 . .
        """) {
            assertEquals(0, it.player1Score)
            assertEquals(0, it.player2Score)

            val moveResult = it.makeMove(3 x 3, Player.First)!!
            val bases = moveResult.bases
            assertEquals(4, bases.size)
            assertEquals(4, it.player1Score)
            assertEquals(0, it.player2Score)
        }
    }

    @Test
    fun baseWithInternalCenterHole() {
        testFieldWithRollback(
            """
            . . + + +
            . . * * * +
            + * * + * * +
            + * + . + * +
            + * * + * * +
            . + * * * +
            . . + + +
        """
        ) {
            val moveResult = it.makeMove(2 x 2, Player.Second)!!
            val base = moveResult.bases.single()
            assertEquals(16, base.playerDiff)
            val (_, innerClosures) = base.getSortedClosurePositions(it)
            val innerClosure = innerClosures.single()
            assertEquals(4, innerClosure.size)
        }
    }

    @Test
    fun baseWithDoubleAdjacentInternalCenterHole() {
        testFieldWithRollback(
            """
            . . + + + + +
            . . * * * * * +
            + * * + * + * * +
            + * + . + . + * +
            + * * + * + * * +
            . + * * * * * +
            . . + + + + +
        """
        ) {
            val moveResult = it.makeMove(2 x 2, Player.Second)!!
            val base = moveResult.bases.single()
            assertEquals(22, base.playerDiff)
            val (_, innerClosures) = base.getSortedClosurePositions(it)
            assertTrue(innerClosures.single().size.let { size -> size == 7 || size == 8 })
        }
    }

    @Test
    fun baseWithDoubleIndependentInternalCenterHoles() {
        testFieldWithRollback(
            """
            . . + + + + + + +
            . . * * * * * * * +
            + * * + * * * + * * +
            + * + . + * + . + * +
            + * * + * * * + * * +
            . + * * * * * * * +
            . . + + + + + + +
        """
        ) {
            val moveResult = it.makeMove(2 x 2, Player.Second)!!
            val base = moveResult.bases.single()
            assertEquals(31, base.playerDiff)
            val (_, innerClosures) = base.getSortedClosurePositions(it)
            assertEquals(2, innerClosures.size)
        }
    }

    @Test
    fun baseWithInternalSingleConnections() {
        testFieldWithRollback(
            """
            . * * * 
            * + + + *
            * + . + *
            * + + + *
            . * * * 
        """
        ) {
            val moveResult = it.makeMove(3 x 3, Player.First)!!
            val base = moveResult.bases.single()
            val (outerClosure, innerClosure) = base.getSortedClosurePositions(it)
            assertEquals(
                listOf(
                    3 x 1,
                    2 x 1,
                    1 x 2,
                    1 x 3,
                    1 x 4,
                    2 x 5,
                    3 x 5,
                    4 x 5,
                    5 x 4,
                    5 x 3,
                    5 x 2,
                    4 x 1,
                ),
                outerClosure
            )
            assertEquals(listOf(3 x 3), innerClosure.single())
        }
    }

    @Test
    fun baseWithInternalDoubleConnections() {
        testFieldWithRollback(
            """
            . * * * *
            * + + + + *
            * + * . + *
            * + + + + *
            . * * * *
        """
        ) {
            val moveResult = it.makeMove(4 x 3, Player.First)!!
            val base = moveResult.bases.single()
            val (outerClosure, innerClosure) = base.getSortedClosurePositions(it)
            assertTrue(outerClosure.isNotEmpty())
            assertEquals(listOf(4 x 3, 3 x 3), innerClosure.single())
        }
    }

    @Test
    fun baseWithInternalTripleConnections() {
        testFieldWithRollback(
            """
            . * * * *
            * + + + + *
            * + * * + *
            * + + . + *
            . * + + + *
            . . * * *
        """
        ) {
            val moveResult = it.makeMove(4 x 4, Player.First)!!
            val base = moveResult.bases.single()
            val (outerClosure, innerClosure) = base.getSortedClosurePositions(it)
            assertTrue(outerClosure.isNotEmpty())
            assertEquals(listOf(3 x 3, 4 x 3, 4 x 4), innerClosure.single())
        }
    }
}