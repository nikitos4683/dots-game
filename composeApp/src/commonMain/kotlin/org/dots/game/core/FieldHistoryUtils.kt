package org.dots.game.core

data class FieldHistoryNodeIndex(val x: Int, val y: Int)

private const val X_OFFSET = 1
private const val Y_OFFSET = 1

fun FieldHistory.getNodeIndexes(mainBranchIsAlwaysStraight: Boolean = false): Map<Node, FieldHistoryNodeIndex> {
    val maxYOffset = mutableListOf<Int>(0)

    return buildMap {
        fun walk(node: Node, xIndex: Int, yIndex: Int) {
            this[node] = FieldHistoryNodeIndex(xIndex, yIndex)

            val values = node.nextNodes.values
            val firstNode = values.firstOrNull()
            val lastNode = values.lastOrNull()
            for (nextNode in values) {
                val nextXIndex = xIndex + X_OFFSET
                if (nextXIndex >= maxYOffset.size) {
                    maxYOffset.add(-1)
                }
                val nextYIndex = maxOf(maxYOffset[nextXIndex] + Y_OFFSET, maxYOffset[xIndex])
                maxYOffset[nextXIndex] = nextYIndex

                if (mainBranchIsAlwaysStraight && nextNode == firstNode && nextYIndex > maxYOffset[xIndex]) {
                    // Realign parent nodes since they are already set up
                    var xIndexOfMainBranch = xIndex
                    var currentNode: Node = nextNode
                    var nodeToRealign: Node = node
                    do {
                        maxYOffset[xIndexOfMainBranch] = nextYIndex
                        this[nodeToRealign] = FieldHistoryNodeIndex(xIndexOfMainBranch, nextYIndex)
                        xIndexOfMainBranch--
                        currentNode = nodeToRealign
                        nodeToRealign = currentNode.previousNode!!
                    } while (nodeToRealign.nextNodes.values.firstOrNull() == currentNode && nextYIndex > maxYOffset[xIndexOfMainBranch])
                    maxYOffset[xIndexOfMainBranch] = nextYIndex // Space is needed for drawing connection lines
                }

                if (nextNode == lastNode) { // Space is needed for drawing connection lines
                    maxYOffset[xIndex] = nextYIndex
                }
                walk(nextNode, nextXIndex, nextYIndex)
            }
        }

        walk(rootNode, 0, 0)
    }
}