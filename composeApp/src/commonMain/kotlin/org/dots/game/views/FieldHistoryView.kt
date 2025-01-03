package org.dots.game.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.dots.game.UiSettings
import org.dots.game.core.FieldHistory
import org.dots.game.core.Node
import org.dots.game.core.getNodeIndexes
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import kotlin.math.round

private val stepSize = 50.0f
private val nodeRatio = 0.33f
private val rootNodeColor = Color.LightGray
private val selectedNodeRectColor = Color.Black
private val lineColor = Color(0f, 0f, 0f, 0.8f)
private val textColor: Color = Color.White

private val padding = stepSize
private val nodeRadius = stepSize * nodeRatio

@Composable
fun FieldHistoryView(
    currentNode: Node?,
    fieldHistory: FieldHistory,
    uiSettings: UiSettings,
    onChangeCurrentNode: () -> Unit
) {
    val textMeasurer = rememberTextMeasurer()
    val indexToNodeMap= fieldHistory.getNodeIndexes(mainBranchIsAlwaysStraight = true)
    val nodeToIndexMap = buildMap {
        for (xIndex in indexToNodeMap.indices) {
            for ((yIndex, node) in indexToNodeMap[xIndex]) {
                this[node] = xIndex to yIndex
            }
        }
    }

    Canvas(
        Modifier.size(calculateSize(indexToNodeMap)).pointerInput(indexToNodeMap, fieldHistory) {
            detectTapGestures(
                onPress = { tapOffset ->
                    if (handleTap(tapOffset, fieldHistory, indexToNodeMap)) {
                        onChangeCurrentNode()
                    }
                }
            )
        },
        contentDescription = "Field History Tree"
    ) {
        drawConnections(indexToNodeMap, nodeToIndexMap)
        drawNodes(indexToNodeMap, textMeasurer, uiSettings)
        drawCurrentNode(currentNode, nodeToIndexMap)
    }
}

private fun calculateSize(indexToNodeMap: List<LinkedHashMap<Int, Node>>): DpSize {
    val maxXIndex = indexToNodeMap.size - 1
    val maxYIndex = indexToNodeMap.maxOf { yIndexes -> yIndexes.maxOf { it.key } }

    //return Size(maxXIndex * stepSize + padding * 2, maxYIndex * stepSize + padding * 2)
    return DpSize((maxXIndex * stepSize + padding * 2).dp, (maxYIndex * stepSize + padding * 2).dp)
}

private fun DrawScope.drawConnections(indexToNodeMap: List<LinkedHashMap<Int, Node>>, nodeToIndexMap: Map<Node, Pair<Int, Int>>) {
    for (xIndex in indexToNodeMap.indices) {
        var previousYIndex = 0
        var previousYNode: Node? = null
        for ((yIndex, node) in indexToNodeMap[xIndex]) {
            val centerOffsetX = xIndex * stepSize + padding
            val centerOffsetY = yIndex * stepSize + padding

            if (node.isRoot) continue // No connection for the root node

            val xLineOffset = (xIndex - 1) * stepSize + padding

            drawLine(lineColor, Offset(xLineOffset, centerOffsetY), Offset(centerOffsetX, centerOffsetY))

            val parentNode = node.previousNode!!
            val parentNodeYIndex = if (previousYNode?.previousNode == parentNode) {
                // Avoid drawing the line multiple times in case of the previous sibling node is already processed
                previousYIndex
            } else {
                nodeToIndexMap.getValue(parentNode).second
            }

            val verticalConnectionLinesCount = yIndex - parentNodeYIndex
            if (verticalConnectionLinesCount > 0) {
                drawLine(
                    lineColor,
                    Offset(xLineOffset, centerOffsetY),
                    Offset(xLineOffset, centerOffsetY - stepSize * verticalConnectionLinesCount)
                )
            }

            previousYIndex = yIndex
            previousYNode = node
        }
    }
}

private fun DrawScope.drawNodes(indexToNodeMap: List<LinkedHashMap<Int, Node>>, textMeasurer: TextMeasurer, uiSettings: UiSettings) {
    for (xIndex in indexToNodeMap.indices) {
        for ((yIndex, node) in indexToNodeMap[xIndex]) {
            val color: Color
            val moveNumber: Int
            if (node.isRoot) {
                color = rootNodeColor
                moveNumber = 0
            } else {
                color = uiSettings.toColor(node.moveResult!!.player)
                moveNumber = node.number
            }

            val centerOffsetX = xIndex * stepSize + padding
            val centerOffsetY = yIndex * stepSize + padding

            drawCircle(color, nodeRadius, Offset(centerOffsetX, centerOffsetY))

            val textLayoutResult = textMeasurer.measure(moveNumber.toString())
            drawText(
                textLayoutResult,
                textColor,
                Offset(
                    centerOffsetX - textLayoutResult.size.width / 2,
                    centerOffsetY - textLayoutResult.size.height / 2
                )
            )
        }
    }
}

private fun DrawScope.drawCurrentNode(currentNode: Node?, nodeToIndexMap: Map<Node, Pair<Int, Int>>) {
    if (currentNode == null) return
    val (xIndex, yIndex) = nodeToIndexMap.getValue(currentNode)
    val centerOffsetX = xIndex * stepSize + padding
    val centerOffsetY = yIndex * stepSize + padding
    drawRect(
        selectedNodeRectColor,
        Offset(centerOffsetX - nodeRadius, centerOffsetY - nodeRadius),
        Size(nodeRadius * 2, nodeRadius * 2),
        style = Stroke(2.0f)
    )
}

private fun handleTap(tapOffset: Offset, fieldHistory: FieldHistory, indexToNodeMap: List<LinkedHashMap<Int, Node>>): Boolean {
    val xIndex = round((tapOffset.x - padding) / stepSize).toInt()
    val yIndexes = indexToNodeMap.getOrNull(xIndex) ?: return false

    val yIndex = round((tapOffset.y - padding) / stepSize).toInt()
    val node = yIndexes[yIndex] ?: return false

    return fieldHistory.switch(node)
}