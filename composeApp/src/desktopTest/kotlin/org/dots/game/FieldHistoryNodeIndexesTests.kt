package org.dots.game

import org.dots.game.core.Field
import org.dots.game.core.FieldHistory
import org.dots.game.core.FieldHistoryNodeIndex
import org.dots.game.core.InitialPosition
import org.dots.game.core.Position
import org.dots.game.core.Rules
import org.dots.game.core.getNodeIndexes
import kotlin.test.Test
import kotlin.test.assertEquals

class FieldHistoryNodeIndexesTests {
    /**
     * # Expected
     *
     * ```
     * 0 - 1
     * |
     * . - 2 - 3
     * ```
     */
    @Test
    fun simple() {
        with (initializeFieldHistory()) {
            makeMove(1, 1)
            back()
            makeMove(2, 2)
            makeMove(3, 3)

            assertEquals(
                listOf(FieldHistoryNodeIndex(0, 0), FieldHistoryNodeIndex(1, 0), FieldHistoryNodeIndex(1, 1), FieldHistoryNodeIndex(2, 1)),
                getNodeIndexes().values.toList()
            )
        }
    }

    /**
     * # Expected
     *
     * ```
     * 0 - 1 - 2 - 3 - 4 - 5
     * |   |           |
     * .   . - 7 - 8   . - 6
     * |           |
     * . - 10      . - 9
     * ```
     */
    @Test
    fun mainBranchIsAlwaysStraightIsDisables() {
        with (initializeFieldHistory()) {
            setUpHistoryForTestsThatShowAffectionOfMainBranchIsAlwaysStraight()

            assertEquals(
                listOf(
                    FieldHistoryNodeIndex(0, 0),
                    FieldHistoryNodeIndex(1, 0),
                    FieldHistoryNodeIndex(2, 0),
                    FieldHistoryNodeIndex(3, 0),
                    FieldHistoryNodeIndex(4, 0),
                    FieldHistoryNodeIndex(5, 0),
                    FieldHistoryNodeIndex(5, 1),
                    FieldHistoryNodeIndex(2, 1),
                    FieldHistoryNodeIndex(3, 1),
                    FieldHistoryNodeIndex(4, 2),
                    FieldHistoryNodeIndex(1, 2),
                ),
                getNodeIndexes().values.toList()
            )
        }
    }

    /**
     * # Expected
     *
     * ```
     * 0 - 1 - 2 - 3 - 4 - 5
     * |   |           |
     * .   .           . - 6
     * |   |
     * .   . - 7 - 8 - 9
     * |
     * . - 10
     * ```
     */
    @Test
    fun mainBranchIsAlwaysStraightIsEnabled() {
        with (initializeFieldHistory()) {
            setUpHistoryForTestsThatShowAffectionOfMainBranchIsAlwaysStraight()

            assertEquals(
                listOf(
                    FieldHistoryNodeIndex(0, 0),
                    FieldHistoryNodeIndex(1, 0),
                    FieldHistoryNodeIndex(2, 0),
                    FieldHistoryNodeIndex(3, 0),
                    FieldHistoryNodeIndex(4, 0),
                    FieldHistoryNodeIndex(5, 0),
                    FieldHistoryNodeIndex(5, 1),
                    FieldHistoryNodeIndex(2, 2),
                    FieldHistoryNodeIndex(3, 2),
                    FieldHistoryNodeIndex(4, 2),
                    FieldHistoryNodeIndex(1, 3),
                ),
                getNodeIndexes(mainBranchIsAlwaysStraight = true).values.toList()
            )
        }
    }

    private fun FieldHistory.setUpHistoryForTestsThatShowAffectionOfMainBranchIsAlwaysStraight() {
        makeMove(1, 1) // 1
        makeMove(2, 1) // 2
        makeMove(3, 1) // 3
        makeMove(4, 1) // 4
        makeMove(5, 1) // 5

        back()
        makeMove(1, 2) // 6

        switch(rootNode)
        next()

        makeMove(1, 3) // 7
        makeMove(2, 3) // 8
        makeMove(3, 3) // 9

        switch(rootNode)
        makeMove(1, 4) // 10
    }

    private fun initializeFieldHistory(): FieldHistory {
        val field = Field(Rules(8, 8, initialPosition = InitialPosition.Empty))
        return FieldHistory(field)
    }

    private fun FieldHistory.makeMove(x: Int, y: Int): Boolean {
        return add(field.makeMoveInternal(Position(x, y))!!)
    }
}