package org.dots.game.core

private const val X_OFFSET = 1
private const val Y_OFFSET = 1

/**
 * @return a structure that represents coordinate of nodes
 * The first `List` stands for X coordinate, it doesn't have gaps.
 * The second `LinkedHashMap` stands for Y coordinate because it could have gaps.
 * However, using `LinkedHashMap` instead of `SortedMap` is valid here
 * because elements are always being added in correct sorted order.
 * Also, `SortedMap` is only available under JVM, but not common source set.
 *
 * @param[mainBranchIsAlwaysStraight] stands for rendering main branches.
 * The main branch is a branch composed of every first next node.
 * If it's `true`, then such branches are rendered as straight lines (see tests for clarity)
 */
fun FieldHistory.getNodeIndexes(mainBranchIsAlwaysStraight: Boolean = false): List<LinkedHashMap<Int, Node>> {
    val maxYIndex = mutableListOf<Int>(0)
    val result = mutableListOf<LinkedHashMap<Int, Node>>()

    fun walk(node: Node, xIndex: Int, yIndex: Int) {
        val yIndexes = if (xIndex >= result.size) {
            LinkedHashMap<Int, Node>().also { result.add(xIndex, it) }
        } else {
            result[xIndex]
        }
        yIndexes[yIndex] = node

        val nextNodes = node.nextNodes.values
        val firstNode = nextNodes.firstOrNull()
        val lastNode = nextNodes.lastOrNull()
        for (nextNode in nextNodes) {
            val nextXIndex = xIndex + X_OFFSET

            val maxYIndexAtXIndex = maxYIndex[xIndex]
            val maxYIndexAtNextXIndex = if (nextXIndex >= maxYIndex.size) {
                // If the next node has max X offset, then initialize max Y to 0
                0.also { maxYIndex.add(it) }
            } else {
                // The next sibling node should be shifted to avoid collision
                maxYIndex[nextXIndex] + Y_OFFSET
            }

            val nextYIndex: Int
            if (maxYIndexAtNextXIndex > maxYIndexAtXIndex) {
                // Increase Y index of the current branch to avoid collision with next nodes
                nextYIndex = maxYIndexAtNextXIndex

                if (mainBranchIsAlwaysStraight && nextNode == firstNode) {
                    // Realign parent nodes using the obtained new offset (`nextYIndex`)
                    var xIndexOfMainBranch = xIndex
                    var yIndexOfMainBranch = maxYIndexAtXIndex
                    var currentNode: Node
                    var nodeToRealign: Node = node
                    do {
                        maxYIndex[xIndexOfMainBranch] = nextYIndex
                        requireNotNull(result[xIndexOfMainBranch].remove(yIndexOfMainBranch))
                        result[xIndexOfMainBranch][nextYIndex] = nodeToRealign

                        xIndexOfMainBranch--
                        yIndexOfMainBranch = maxYIndex[xIndexOfMainBranch]
                        currentNode = nodeToRealign
                        nodeToRealign = currentNode.previousNode!!
                    } while (nodeToRealign.nextNodes.values.firstOrNull() == currentNode && nextYIndex > yIndexOfMainBranch)
                    maxYIndex[xIndexOfMainBranch] = nextYIndex // Leave space for branching to draw connection lines
                }
            } else {
                // Not increase the Y index, but keep the index of the current branch,
                nextYIndex = maxYIndexAtXIndex
            }
            maxYIndex[nextXIndex] = nextYIndex

            if (nextNode == lastNode) {
                // Leave space for branching to draw connection lines
                maxYIndex[xIndex] = nextYIndex
            }

            walk(nextNode, nextXIndex, nextYIndex)
        }
    }

    walk(rootNode, 0, 0)

    return result
}