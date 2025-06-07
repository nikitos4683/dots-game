package org.dots.game.field

import org.dots.game.core.BaseMode
import org.dots.game.core.DotState
import org.dots.game.core.Field
import org.dots.game.core.MoveInfo
import org.dots.game.core.Position
import org.dots.game.core.Rules
import org.dots.game.dump.FieldParser
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

abstract class FieldTests {
    open val captureByBorder: Boolean = Rules.Standard.captureByBorder
    open val baseMode: BaseMode = Rules.Standard.baseMode
    open val suicideAllowed: Boolean = Rules.Standard.suicideAllowed
    open val initialMoves: List<MoveInfo> = listOf()

    fun initRules(width: Int, height: Int): Rules =  Rules(width, height, captureByBorder, baseMode, suicideAllowed, initialMoves)

    fun testFieldWithRollback(fieldData: String, check: (Field) -> Unit) {
        with (FieldParser.parseAndConvert(fieldData, initializeRules = { width, height -> initRules(width, height) })) {
            check(this)
            unmakeAllMovesAndCheck()
        }
    }

    protected fun Field.unmakeAllMovesAndCheck() {
        unmakeAllMoves()

        assertTrue(moveSequence.size == initialMovesCount)
        assertEquals(0, player1Score)
        assertEquals(0, player2Score)
        assertNull(gameResult)
        assertEquals(width * height - initialMovesCount, numberOfLegalMoves)
        var actualInitialMovesCount = 0
        for (x in 0 until realWidth) {
            for (y in 0 until realHeight) {
                val position = Position(x, y)
                val borderOrEmptyState = if (captureByBorder && position.isBorder()) DotState.Border else DotState.Empty
                if (borderOrEmptyState != Position(x, y).getState()) {
                    actualInitialMovesCount++
                }
            }
        }

        assertEquals(initialMovesCount, actualInitialMovesCount)
    }
}