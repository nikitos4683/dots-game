package org.dots.game

import org.dots.game.core.Field
import org.dots.game.core.FieldHistory
import org.dots.game.core.InitialPosition
import org.dots.game.core.Node
import org.dots.game.core.Position
import org.dots.game.core.Rules
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FieldHistoryTests : FieldTests() {
    private val field = Field(Rules(4, 4, initialPosition = InitialPosition.Empty))
    private val fieldHistory = FieldHistory(field)

    @Test
    fun addBackNextCommands() {
        assertTrue(fieldHistory.add(field.makeMoveInternal(Position(1, 1))!!))
        assertTrue(fieldHistory.back())
        assertFalse(fieldHistory.back()) // False because there are no more moves
        assertFalse(fieldHistory.add(field.makeMoveInternal(Position(1, 1))!!)) // False, because such a node already exists
        assertTrue(fieldHistory.back())
        assertTrue(fieldHistory.add(field.makeMoveInternal(Position(2, 1))!!)) // True, because it's a new node
        assertTrue(fieldHistory.back())
        assertTrue(fieldHistory.next()) // True, the current node should be (1;1) (first branch)
        assertEquals(Position(1, 1), field.lastMove!!.position)
        assertFalse(fieldHistory.next()) // False, there are no more moves
    }

    @Test
    fun switchOnTheSameNode() {
        fieldHistory.add(field.makeMoveInternal(Position(1, 1))!!)
        val currentNode = fieldHistory.currentNode
        assertFalse(fieldHistory.switch(currentNode)) // No switch on the same node
    }

    @Test
    fun switchOnPreviousAndNextNode() {
        fieldHistory.add(field.makeMoveInternal(Position(1, 1))!!)
        val firstMoveNode = fieldHistory.currentNode

        fieldHistory.add(field.makeMoveInternal(Position(2, 1))!!)
        fieldHistory.add(field.makeMoveInternal(Position(2, 2))!!)
        val lastMoveNode = fieldHistory.currentNode

        assertTrue(fieldHistory.switch(firstMoveNode)) // Only rollback 2 nodes
        assertEquals(firstMoveNode, fieldHistory.currentNode)

        assertTrue(fieldHistory.switch(lastMoveNode)) // Only push 2 nodes
        assertEquals(lastMoveNode, fieldHistory.currentNode)
    }

    @Test
    fun switchOnDifferentBranches() {
        fieldHistory.add(field.makeMoveInternal(Position(1, 1))!!)
        val firstMoveNode = fieldHistory.currentNode

        fieldHistory.add(field.makeMoveInternal(Position(2, 1))!!)
        fieldHistory.back()
        fieldHistory.back()

        fieldHistory.add(field.makeMoveInternal(Position(2, 2))!!)
        fieldHistory.add(field.makeMoveInternal(Position(3, 3))!!)

        assertTrue(fieldHistory.switch(firstMoveNode)) // Rollback 2 nodes and add 1 one
        assertEquals(firstMoveNode, fieldHistory.currentNode)
    }

    @Test
    fun switchOnUnrelatedNode() {
        fieldHistory.add(field.makeMoveInternal(Position(1, 1))!!)
        fieldHistory.add(field.makeMoveInternal(Position(2, 2))!!)

        assertFalse(fieldHistory.switch(Node(null, null, 0))) // Try switching to unrelated node, no change
    }
}