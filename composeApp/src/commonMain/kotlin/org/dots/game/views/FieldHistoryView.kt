package org.dots.game.views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import org.dots.game.UiSettings
import org.dots.game.core.*

private val stepSize = 40.dp
private val nodeRatio = 0.33f
private val rootNodeColor = Color.LightGray
private val selectedNodeRectColor = Color.Black
private val lineColor = Color(0f, 0f, 0f, 0.8f)
private val textColor: Color = Color.White
private val lineThickness = 1.dp
private val linesZIndex = 0f
private val nodesZIndex = 1f
private val selectedNodeZIndex = 2f

private val nodeRadius = stepSize * nodeRatio
private val nodeSize = nodeRadius * 2

@Composable
fun FieldHistoryView(
    currentNode: Node?,
    fieldHistory: FieldHistory,
    fieldHistoryViewData: FieldHistoryViewData,
    uiSettings: UiSettings,
    onChangeCurrentNode: () -> Unit
) {
    val requester = remember { FocusRequester() }
    Box(Modifier.size(300.dp, 300.dp).padding(stepSize)
        .focusRequester(requester)
        .focusable()
        .onKeyEvent { keyEvent ->
            handleKeyEvent(keyEvent, fieldHistory).also {
                if (it) {
                    onChangeCurrentNode()
                }
            }
        }
    ) {
        ConnectionsAndNodes(fieldHistory, fieldHistoryViewData.fieldHistoryElements, onChangeCurrentNode, uiSettings)
        CurrentNode(currentNode, fieldHistoryViewData.nodeToIndexMap)
    }
    LaunchedEffect(fieldHistory) {
        requester.requestFocus()
    }
}

@Composable
private fun ConnectionsAndNodes(
    fieldHistory: FieldHistory,
    fieldHistoryElements: FieldHistoryElements,
    onChangeCurrentNode: () -> Unit,
    uiSettings: UiSettings
) {
    for (xIndex in fieldHistoryElements.indices) {
        for ((yIndex, element) in fieldHistoryElements[xIndex].withIndex()) {

            val centerOffsetX = stepSize * xIndex
            val centerOffsetY = stepSize * yIndex

            when (element) {
                is NodeHistoryElement -> {
                    val node = element.node

                    val color: Color
                    val moveNumber: Int
                    if (node.isRoot) {
                        color = rootNodeColor
                        moveNumber = 0
                    } else {
                        color = uiSettings.toColor(node.moveResult!!.player)
                        moveNumber = node.number

                        // Render horizontal connection line
                        Box(
                            Modifier
                                .offset(stepSize * (xIndex - 1), centerOffsetY - lineThickness / 2)
                                .size(stepSize, lineThickness)
                                .background(lineColor)
                                .zIndex(linesZIndex)
                        )
                    }

                    // Render node
                    Box(
                        Modifier
                            .offset(centerOffsetX - nodeRadius, centerOffsetY - nodeRadius)
                            .size(nodeSize, nodeSize)
                            .clip(CircleShape)
                            .background(color)
                            .zIndex(nodesZIndex)
                            .pointerInput(Unit) {
                                detectTapGestures(onPress = {
                                    if (fieldHistory.switch(node)) {
                                        onChangeCurrentNode()
                                    }
                                })
                            }
                    ) {
                        Text(moveNumber.toString(), Modifier.align(Alignment.Center), textColor)
                    }
                }

                is VerticalLineHistoryElement -> {
                    // Render vertical connection line
                    Box(
                        Modifier
                            .offset(stepSize * xIndex - lineThickness / 2, centerOffsetY - stepSize)
                            .size(lineThickness, stepSize)
                            .background(lineColor)
                            .zIndex(linesZIndex)
                    )
                }

                is EmptyHistoryElement -> {}
            }
        }
    }
}

@Composable
private fun CurrentNode(currentNode: Node?, nodeToIndexMap: Map<Node, Pair<Int, Int>>) {
    if (currentNode == null) return
    val (xIndex, yIndex) = nodeToIndexMap.getValue(currentNode)

    val centerOffsetX = stepSize * xIndex - nodeRadius
    val centerOffsetY = stepSize * yIndex - nodeRadius
    val size = nodeRadius * 2

    Box(
        Modifier
            .offset(centerOffsetX, centerOffsetY)
            .size(size, size)
            .border(1.dp, selectedNodeRectColor)
            .zIndex(selectedNodeZIndex)
    )
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