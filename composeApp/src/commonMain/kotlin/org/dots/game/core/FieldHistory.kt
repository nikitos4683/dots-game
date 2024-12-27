package org.dots.game.core

class FieldHistory(val field: Field) {
    val firstNode: Node = Node(null, null, 0, mutableMapOf())
    var currentNode: Node = firstNode
        private set

    /**
     * Returns `false` if such a node with the current @param[move] already exists,
     * otherwise returns `true`
     */
    fun add(move: MoveResult): Boolean {
        val position = move.position
        val existingNode = currentNode.nextNodes[position]

        var result: Boolean
        currentNode = if (existingNode == null) {
            result = true
            Node(move, previousNode = currentNode, move.number - field.initialMovesCount + 1).also { currentNode.nextNodes[position] = it }
        } else {
            result = false
            existingNode
        }

        return result
    }

    /**
     * Returns `false` if the @property[currentNode] is root, otherwise returns `true`
     */
    fun back(): Boolean {
        val previousNode = currentNode.previousNode ?: return false
        field.unmakeMove()
        currentNode = previousNode
        return true
    }

    /**
     * Returns `false` is there are no next nodes, otherwise returns `true`
     */
    fun next(): Boolean {
        val nextNode = currentNode.nextNodes.values.firstOrNull() ?: return false
        val moveResult = nextNode.moveResult!!
        field.makeMoveInternal(moveResult.position, moveResult.player)
        currentNode = nextNode
        return true
    }

    fun switch(targetNode: Node): Boolean {
        if (targetNode == currentNode) return false

        val reversedCurrentNodes = buildMap {
            currentNode.walkMovesReversed { node, distance ->
                this[node] = distance
                return@walkMovesReversed node != targetNode
            }
        }

        var numberOrMovesToRollback: Int? = null
        val nextNodes = buildList {
            targetNode.walkMovesReversed { node, _ ->
                val potentialNumberOfMovesToRollback = reversedCurrentNodes[node]
                if (potentialNumberOfMovesToRollback != null) {
                    numberOrMovesToRollback = potentialNumberOfMovesToRollback
                    false
                } else {
                    add(node)
                    true
                }
            }
        }.reversed()

        if (numberOrMovesToRollback == null) return false // No common root -> the `targetNode` is unrelated

        (0 until numberOrMovesToRollback).forEach { i ->
            requireNotNull(field.unmakeMoveInternal())
            currentNode = currentNode.previousNode!!
        }

        for (nextNode in nextNodes) {
            val moveResult = nextNode.moveResult ?: continue
            val nextMovePosition = moveResult.position
            requireNotNull(field.makeMoveInternal(nextMovePosition, moveResult.player))
            currentNode = currentNode.nextNodes.getValue(moveResult.position)
        }

        require(currentNode == targetNode)

        return true
    }

    private inline fun Node.walkMovesReversed(action: (Node, Int) -> Boolean) {
        var move = this
        var distance = 0
        do {
            if (!action(move, distance)) return
            distance++
            move = move.previousNode ?: break
        }
        while (true)
    }
}

class Node(
    val moveResult: MoveResult?,
    val previousNode: Node?,
    val number: Int,
    val nextNodes: MutableMap<Position, Node> = mutableMapOf(),
) {
    val isRoot = moveResult == null

    override fun toString(): String {
        return moveResult?.toString() ?: "root"
    }
}
