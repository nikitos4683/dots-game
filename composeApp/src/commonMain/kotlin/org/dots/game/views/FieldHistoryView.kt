package org.dots.game.views

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.dots.game.UiSettings
import org.dots.game.core.FieldHistory
import org.dots.game.core.Node
import kotlin.collections.iterator
import kotlin.math.round

private typealias NodePosition = Pair<Int, Int>

class FieldHistoryView(
    val fieldHistory: FieldHistory,
    val textMeasurer: TextMeasurer,
    val uiSettings: UiSettings,
) {
    val stepSize = 50.0f
    val nodeRatio = 0.33f
    val rootNodeColor = Color.LightGray
    val selectedNodeRectColor = Color.Black
    val lineColor = Color.Black
    val textColor = Color.White

    val padding = stepSize
    val nodeRadius = stepSize * nodeRatio

    private val nodeToPositionMap = mutableMapOf<NodePosition, Node>()
    private val positionToNode = mutableMapOf<Node, NodePosition>()

    fun calculateSize(): DpSize {
        recalculateMaps()

        val maxXIndex = positionToNode.values.maxOf { it.first }
        val maxYIndex = positionToNode.values.maxOf { it.second }

        //return Size(maxXIndex * stepSize + padding * 2, maxYIndex * stepSize + padding * 2)
        return DpSize((maxXIndex * stepSize + padding * 2).dp, (maxYIndex * stepSize + padding * 2).dp)
    }

    fun draw(drawScope: DrawScope) {
        drawScope.drawConnections()
        drawScope.drawNodes()
    }

    private fun DrawScope.drawConnections() {
        for ((position, node) in nodeToPositionMap) {
            val (xIndex, yIndex) = position

            val centerOffsetX = xIndex * stepSize + padding
            val centerOffsetY = yIndex * stepSize + padding

            if (node.isRoot) continue // No connection for the root node

            val xLineOffset = (xIndex - 1) * stepSize + padding

            drawLine(lineColor, Offset(xLineOffset, centerOffsetY), Offset(centerOffsetX, centerOffsetY))

            val previousPosition = positionToNode.getValue(node.previousNode!!)
            val verticalConnectionLinesCount = yIndex - previousPosition.second

            if (verticalConnectionLinesCount > 0) {
                drawLine(
                    lineColor,
                    Offset(xLineOffset, centerOffsetY),
                    Offset(xLineOffset, centerOffsetY - stepSize * verticalConnectionLinesCount)
                )
            }
        }
    }

    private fun DrawScope.drawNodes() {
        for ((position, node) in nodeToPositionMap) {
            val (xIndex, yIndex) = position

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
                    centerOffsetX - textLayoutResult.size.width / 2, centerOffsetY - textLayoutResult.size.height / 2
                )
            )

            if (node == fieldHistory.currentNode) {
                drawRect(
                    selectedNodeRectColor,
                    Offset(centerOffsetX - nodeRadius, centerOffsetY - nodeRadius),
                    Size(nodeRadius * 2, nodeRadius * 2),
                    style = Stroke(2.0f)
                )
            }
        }
    }

    private fun recalculateMaps() {
        nodeToPositionMap.clear()
        positionToNode.clear()

        val yOffset = mutableListOf<Int>(0)

        fun walk(node: Node, xIndex: Int, yIndex: Int) {
            val xyPair = xIndex to yIndex
            nodeToPositionMap[xyPair] = node
            positionToNode[node] = xyPair

            var nextYIndex: Int
            val values = node.nextNodes.values
            val lastNode = values.lastOrNull()
            for (nextNode in values) {
                val nextXIndex = xIndex + 1
                if (nextXIndex >= yOffset.size) {
                    yOffset.add(-1)
                }
                nextYIndex = maxOf(yOffset[nextXIndex] + 1, yOffset[xIndex])
                yOffset[nextXIndex] = nextYIndex
                if (nextNode == lastNode) { // Space is needed for drawing connection lines
                    yOffset[xIndex] = nextYIndex
                }
                walk(nextNode, nextXIndex, nextYIndex)
            }
        }

        walk(fieldHistory.firstNode, 0, 0)
    }

    fun handleTap(tapOffset: Offset): Boolean {
        val xIndex = round((tapOffset.x - padding) / stepSize).toInt()
        val yIndex = round((tapOffset.y - padding) / stepSize).toInt()

        nodeToPositionMap[xIndex to yIndex]?.let {
            return fieldHistory.switch(it)
        }

        return false
    }

    fun handleKey(keyEvent: KeyEvent): Boolean {
        if (keyEvent.type == KeyEventType.KeyUp) {
            if (keyEvent.key == Key.DirectionLeft) {
                return fieldHistory.back()
            }
        }

        return false
    }
}