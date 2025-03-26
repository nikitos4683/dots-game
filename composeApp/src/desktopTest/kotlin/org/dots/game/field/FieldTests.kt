package org.dots.game.field

import org.dots.game.core.BaseMode
import org.dots.game.core.DotState
import org.dots.game.core.Field
import org.dots.game.core.MoveInfo
import org.dots.game.core.Position
import org.dots.game.core.Rules
import org.dots.game.infrastructure.TestDataParser
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

abstract class FieldTests {
    open val captureByBorder: Boolean = Rules.Standard.captureByBorder
    open val baseMode: BaseMode = Rules.Standard.baseMode
    open val suicideAllowed: Boolean = Rules.Standard.suicideAllowed
    open val initialMoves: List<MoveInfo> = listOf()

    fun initRules(width: Int, height: Int): Rules =  Rules(width, height, captureByBorder, baseMode, suicideAllowed, initialMoves)

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
        val testDataField = TestDataParser.parse(fieldData)
        val field = Field(initRules(testDataField.width, testDataField.height))
        for ((index, testMove) in testDataField.moves.withIndex()) {
            val position = testMove.position
            assertNotNull(field.makeMoveUnsafe(position, testMove.player), "Can't make move #$index to $position")
        }
        return field
    }
}