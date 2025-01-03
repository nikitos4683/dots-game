package org.dots.game

import org.dots.game.core.Field
import org.dots.game.core.FieldHistory
import org.dots.game.core.InitialPosition
import org.dots.game.core.Node
import org.dots.game.core.Position
import org.dots.game.core.Rules
import org.dots.game.core.getNodeIndexes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
            val node0 = rootNode
            val node1 = makeMove(1, 1)
            back()
            val node2 = makeMove(2, 2)
            val node3 = makeMove(3, 3)

            assertEquals(
                listOf(
                    mapOf(0 to node0), // x0
                    mapOf(0 to node1, 1 to node2), // x1
                    mapOf(1 to node3) // x2
                ),
                getNodeIndexes()
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
            val expectedNodes = setUpHistoryForTestsThatShowAffectionOfMainBranchIsAlwaysStraight()

            assertEquals(
                listOf(
                    mapOf(0 to expectedNodes[0]), // x0
                    mapOf(0 to expectedNodes[1], 2 to expectedNodes[10]), // x1
                    mapOf(0 to expectedNodes[2], 1 to expectedNodes[7]), // x2
                    mapOf(0 to expectedNodes[3], 1 to expectedNodes[8]), // x3
                    mapOf(0 to expectedNodes[4], 2 to expectedNodes[9]), // x4
                    mapOf(0 to expectedNodes[5], 1 to expectedNodes[6]), // x4
                ),
                getNodeIndexes()
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
            val expectedNodes = setUpHistoryForTestsThatShowAffectionOfMainBranchIsAlwaysStraight()

            assertEquals(
                listOf(
                    mapOf(0 to expectedNodes[0]), // x0
                    mapOf(0 to expectedNodes[1], 3 to expectedNodes[10]), // x1
                    mapOf(0 to expectedNodes[2], 2 to expectedNodes[7]), // x2
                    mapOf(0 to expectedNodes[3], 2 to expectedNodes[8]), // x3
                    mapOf(0 to expectedNodes[4], 2 to expectedNodes[9]), // x4
                    mapOf(0 to expectedNodes[5], 1 to expectedNodes[6]), // x4
                ),
                getNodeIndexes(mainBranchIsAlwaysStraight = true)
            )
        }
    }

    private fun FieldHistory.setUpHistoryForTestsThatShowAffectionOfMainBranchIsAlwaysStraight(): List<Node> {
        return buildList {
            add(rootNode)       // 0
            add(makeMove(1, 1)) // 1
            add(makeMove(2, 1)) // 2
            add(makeMove(3, 1)) // 3
            add(makeMove(4, 1)) // 4
            add(makeMove(5, 1)) // 5

            back()
            add(makeMove(1, 2)) // 6

            switch(rootNode)
            next()

            add(makeMove(1, 3)) // 7
            add(makeMove(2, 3)) // 8
            add(makeMove(3, 3)) // 9

            switch(rootNode)
            add(makeMove(1, 4)) // 10
        }
    }

    private fun initializeFieldHistory(): FieldHistory {
        val field = Field(Rules(8, 8, initialPosition = InitialPosition.Empty))
        return FieldHistory(field)
    }

    private fun FieldHistory.makeMove(x: Int, y: Int): Node {
        assertTrue(add(field.makeMoveInternal(Position(x, y))!!))
        return currentNode
    }
}