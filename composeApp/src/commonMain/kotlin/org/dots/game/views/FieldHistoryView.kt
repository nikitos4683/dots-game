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
import org.dots.game.core.FieldHistoryNodeIndex
import org.dots.game.core.Node
import org.dots.game.core.getNodeIndexes
import kotlin.collections.iterator
import kotlin.math.round

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

    private var nodeToIndexMap: Map<Node, FieldHistoryNodeIndex> = emptyMap()
    private var indexToNodeMap: Map<FieldHistoryNodeIndex, Node> = emptyMap()

    fun calculateSize(): DpSize {
        nodeToIndexMap = fieldHistory.getNodeIndexes(mainBranchIsAlwaysStraight = false)
        indexToNodeMap = buildMap { nodeToIndexMap.forEach { this[it.value] = it.key } }

        val maxXIndex = nodeToIndexMap.values.maxOf { it.x }
        val maxYIndex = nodeToIndexMap.values.maxOf { it.y }

        //return Size(maxXIndex * stepSize + padding * 2, maxYIndex * stepSize + padding * 2)
        return DpSize((maxXIndex * stepSize + padding * 2).dp, (maxYIndex * stepSize + padding * 2).dp)
    }

    fun draw(drawScope: DrawScope) {
        drawScope.drawConnections()
        drawScope.drawNodes()
    }

    private fun DrawScope.drawConnections() {
        for ((node, index) in nodeToIndexMap) {
            val (xIndex, yIndex) = index

            val centerOffsetX = xIndex * stepSize + padding
            val centerOffsetY = yIndex * stepSize + padding

            if (node.isRoot) continue // No connection for the root node

            val xLineOffset = (xIndex - 1) * stepSize + padding

            drawLine(lineColor, Offset(xLineOffset, centerOffsetY), Offset(centerOffsetX, centerOffsetY))

            val previousIndex = nodeToIndexMap.getValue(node.previousNode!!)
            val verticalConnectionLinesCount = yIndex - previousIndex.y

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
        for ((node, index) in nodeToIndexMap) {
            val (xIndex, yIndex) = index

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

    fun handleTap(tapOffset: Offset): Boolean {
        val xIndex = round((tapOffset.x - padding) / stepSize).toInt()
        val yIndex = round((tapOffset.y - padding) / stepSize).toInt()

        indexToNodeMap[FieldHistoryNodeIndex(xIndex, yIndex)]?.let {
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