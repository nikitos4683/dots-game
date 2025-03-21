package org.dots.game.field

import org.dots.game.core.DotState
import org.dots.game.core.Field
import org.dots.game.core.Position
import org.dots.game.core.Rules
import org.dots.game.infrastructure.TestDataParser
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

abstract class FieldTests {
    open val captureByBorder: Boolean = Rules.Standard.captureByBorder
    open val captureEmptyBase: Boolean = Rules.Standard.captureEmptyBase
    open val suicideAllowed: Boolean = Rules.Standard.suicideAllowed

    fun testFieldWithRollback(fieldData: String, check: (Field) -> Unit) {
        with (initialize(fieldData)) {
            check(this)
            unmakeAllMoves()

            assertTrue(moveSequence.isEmpty())
            assertEquals(0, player1Score)
            assertEquals(0, player2Score)
            assertTrue(emptyBasePositionsSequence.isEmpty())
            for (x in 0 until realWidth) {
                for (y in 0 until realHeight) {
                    val position = Position(x, y)
                    val expectedState = if (captureByBorder && position.isBorder()) DotState.Border else DotState.Empty
                    assertEquals(expectedState, Position(x, y).getState())
                }
            }
        }
    }

    fun initialize(fieldData: String): Field {
        val testDataFiled = TestDataParser.parse(fieldData)
        val rules = Rules(testDataFiled.width, testDataFiled.height, captureByBorder, captureEmptyBase, suicideAllowed)
        val field = Field(rules)
        for ((index, testMove) in testDataFiled.moves.withIndex()) {
            val position = testMove.position
            assertNotNull(field.makeMoveUnsafe(position, testMove.player), "Can't make move #$index to $position")
        }
        return field
    }
}