package org.dots.game.views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.dots.game.UiSettings
import org.dots.game.core.EmptyHistoryElement
import org.dots.game.core.FieldHistory
import org.dots.game.core.FieldHistoryElements
import org.dots.game.core.Node
import org.dots.game.core.NodeHistoryElement
import org.dots.game.core.VerticalLineHistoryElement
import kotlin.math.round

private val stepSize = 40.dp
private val nodeRatio = 0.33f
private val rootNodeColor = Color.LightGray
private val selectedNodeRectColor = Color.Black
private val lineColor = Color(0f, 0f, 0f, 0.8f)
private val textColor: Color = Color.White
private val lineThickness = 1.dp

private val padding = stepSize
private val nodeRadius = stepSize * nodeRatio

@Composable
fun FieldHistoryView(
    currentNode: Node?,
    fieldHistory: FieldHistory,
    fieldHistoryViewData: FieldHistoryViewData,
    uiSettings: UiSettings,
    textMeasurer: TextMeasurer,
    onChangeCurrentNode: () -> Unit
) {
    val requester = remember { FocusRequester() }
    val currentDensity = LocalDensity.current
    Box(Modifier.size(calculateSize(fieldHistoryViewData.fieldHistoryElements))
        .focusRequester(requester).focusable()
        .onKeyEvent { keyEvent -> handleKeyEvent(keyEvent, fieldHistory).also {
            if (it) {
                onChangeCurrentNode()
            }
        } }
        .pointerInput(fieldHistory, fieldHistoryViewData) {
        detectTapGestures(
            onPress = { tapOffset ->
                if (handleTap(tapOffset, fieldHistory, fieldHistoryViewData.fieldHistoryElements, currentDensity)) {
                    onChangeCurrentNode()
                }
            }
        ) }
    ) {
        Connections(fieldHistoryViewData.fieldHistoryElements)
        Nodes(fieldHistoryViewData.fieldHistoryElements, textMeasurer, uiSettings)
        CurrentNode(currentNode, fieldHistoryViewData.nodeToIndexMap)
    }
    LaunchedEffect(fieldHistory) {
        requester.requestFocus()
    }
}

fun calculateSize(fieldHistoryElements: FieldHistoryElements): DpSize {
    val maxXIndex = fieldHistoryElements.size - 1
    val maxYIndex = fieldHistoryElements.maxOf { yLine -> yLine.size } - 1

    return DpSize(stepSize * maxXIndex + padding * 2, stepSize * maxYIndex + padding * 2)
}

@Composable
private fun Connections(fieldHistoryElements: FieldHistoryElements) {
    for (xIndex in fieldHistoryElements.indices) {
        for ((yIndex, element) in fieldHistoryElements[xIndex].withIndex()) {
            with(LocalDensity.current) {
                val centerOffsetY = stepSize * yIndex + padding

                when (element) {
                    is NodeHistoryElement -> {
                        val node = element.node

                        if (node.isRoot) continue // No connection for the root node

                        val xLineOffset = stepSize * (xIndex - 1)  + padding

                        Box(Modifier.offset(xLineOffset, centerOffsetY - lineThickness / 2).size(stepSize, lineThickness).background(lineColor))
                    }

                    is VerticalLineHistoryElement -> {
                        val xLineOffset = stepSize * xIndex + padding

                        Box(Modifier.offset(xLineOffset - lineThickness / 2, centerOffsetY - stepSize).size(lineThickness, stepSize).background(lineColor))
                    }

                    is EmptyHistoryElement -> {}
                }
            }
        }
    }
}

@Composable
private fun Nodes(fieldHistoryElements: FieldHistoryElements, textMeasurer: TextMeasurer, uiSettings: UiSettings) {
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

            with(LocalDensity.current) {
                val centerOffsetX = stepSize * xIndex + padding - nodeRadius
                val centerOffsetY = stepSize * yIndex + padding - nodeRadius
                val size = nodeRadius * 2

                Box(Modifier.offset(centerOffsetX, centerOffsetY).size( size, size).clip(CircleShape).background(color)) {
                    Text(moveNumber.toString(), Modifier.align(Alignment.Center), textColor)
                }
            }
        }
    }
}

@Composable
private fun CurrentNode(currentNode: Node?, nodeToIndexMap: Map<Node, Pair<Int, Int>>) {
    if (currentNode == null) return
    val (xIndex, yIndex) = nodeToIndexMap.getValue(currentNode)

    with(LocalDensity.current) {
        val centerOffsetX = stepSize * xIndex  + padding - nodeRadius
        val centerOffsetY = stepSize * yIndex + padding - nodeRadius
        val size = nodeRadius * 2

        Box(Modifier.offset(centerOffsetX, centerOffsetY).size(size, size).border(width = 2.0f.toDp(), selectedNodeRectColor))
    }
}

private fun handleTap(tapOffset: Offset, fieldHistory: FieldHistory, fieldHistoryElements: FieldHistoryElements, currentDensity: Density): Boolean {
    with (currentDensity) {
        val xIndex = round((tapOffset.x.toDp() - padding) / stepSize).toInt()
        val yLine = fieldHistoryElements.getOrNull(xIndex) ?: return false

        val yIndex = round((tapOffset.y.toDp() - padding) / stepSize).toInt()
        val node = (yLine.getOrNull(yIndex) as? NodeHistoryElement)?.node ?: return false

        return fieldHistory.switch(node)
    }
}

private fun handleKeyEvent(keyEvent: KeyEvent, fieldHistory: FieldHistory): Boolean {
    if (keyEvent.type == KeyEventType.KeyDown) {
        if (keyEvent.key == Key.DirectionLeft) {
            return fieldHistory.back()
        } else if (keyEvent.key == Key.DirectionRight) {
            return fieldHistory.next()
        }
    }

    return false
}