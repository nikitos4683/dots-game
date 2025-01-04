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
import org.dots.game.core.EmptyHistoryElement
import org.dots.game.core.FieldHistory
import org.dots.game.core.FieldHistoryElements
import org.dots.game.core.Node
import org.dots.game.core.NodeHistoryElement
import org.dots.game.core.VerticalLineHistoryElement
import org.dots.game.core.getHistoryElements
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
    val fieldHistoryElements= fieldHistory.getHistoryElements(mainBranchIsAlwaysStraight = true)
    val nodeToIndexMap = buildMap {
        for (xIndex in fieldHistoryElements.indices) {
            for ((yIndex, element) in fieldHistoryElements[xIndex].withIndex()) {
                val node = (element as? NodeHistoryElement)?.node ?: continue
                this[node] = xIndex to yIndex
            }
        }
    }

    Canvas(
        Modifier.size(calculateSize(fieldHistoryElements)).pointerInput(fieldHistoryElements, fieldHistory) {
            detectTapGestures(
                onPress = { tapOffset ->
                    if (handleTap(tapOffset, fieldHistory, fieldHistoryElements)) {
                        onChangeCurrentNode()
                    }
                }
            )
        },
        contentDescription = "Field History Tree"
    ) {
        drawConnections(fieldHistoryElements)
        drawNodes(fieldHistoryElements, textMeasurer, uiSettings)
        drawCurrentNode(currentNode, nodeToIndexMap)
    }
}

private fun calculateSize(fieldHistoryElements: FieldHistoryElements): DpSize {
    val maxXIndex = fieldHistoryElements.size - 1
    val maxYIndex = fieldHistoryElements.maxOf { yLine -> yLine.size } - 1

    //return Size(maxXIndex * stepSize + padding * 2, maxYIndex * stepSize + padding * 2)
    return DpSize((maxXIndex * stepSize + padding * 2).dp, (maxYIndex * stepSize + padding * 2).dp)
}

private fun DrawScope.drawConnections(fieldHistoryElements: FieldHistoryElements) {
    for (xIndex in fieldHistoryElements.indices) {
        for ((yIndex, element) in fieldHistoryElements[xIndex].withIndex()) {
            val centerOffsetX = xIndex * stepSize + padding
            val centerOffsetY = yIndex * stepSize + padding

            when (element) {
                is NodeHistoryElement -> {
                    val node = element.node

                    if (node.isRoot) continue // No connection for the root node

                    val xLineOffset = (xIndex - 1) * stepSize + padding

                    drawLine(lineColor, Offset(xLineOffset, centerOffsetY), Offset(centerOffsetX, centerOffsetY))
                }
                is VerticalLineHistoryElement -> {
                    val xLineOffset = xIndex * stepSize + padding

                    drawLine(
                        lineColor,
                        Offset(xLineOffset, centerOffsetY),
                        Offset(xLineOffset, centerOffsetY - stepSize)
                    )
                }
                is EmptyHistoryElement -> {}
            }
        }
    }
}

private fun DrawScope.drawNodes(fieldHistoryElements: FieldHistoryElements, textMeasurer: TextMeasurer, uiSettings: UiSettings) {
    for (xIndex in fieldHistoryElements.indices) {
        for ((yIndex, element) in fieldHistoryElements[xIndex].withIndex()) {
            val node = (element as? NodeHistoryElement)?.node ?: continue

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

private fun handleTap(tapOffset: Offset, fieldHistory: FieldHistory, fieldHistoryElements: FieldHistoryElements): Boolean {
    val xIndex = round((tapOffset.x - padding) / stepSize).toInt()
    val yLine = fieldHistoryElements.getOrNull(xIndex) ?: return false

    val yIndex = round((tapOffset.y - padding) / stepSize).toInt()
    val node = (yLine.getOrNull(yIndex) as? NodeHistoryElement)?.node ?: return false

    return fieldHistory.switch(node)
}