package org.dots.game.views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import org.dots.game.UiSettings
import org.dots.game.core.*

private val stepSize = 40.dp
private val nodeRatio = 0.33f
private val rootNodeColor = Color.LightGray
private val lineColor = Color(0f, 0f, 0f, 0.8f)
private val textColor: Color = Color.White
private val lineThickness = 1.dp

private val nodeRadius = stepSize * nodeRatio
private val nodeSize = nodeRadius * 2

private val horizontalLineModifier = Modifier
    .size(stepSize, lineThickness)
    .background(lineColor)
    .zIndex(0f)

private val verticalLineModifier = Modifier
    .size(lineThickness, stepSize)
    .background(lineColor)
    .zIndex( 0f)

private val nodeModifier = Modifier
    .size(nodeSize, nodeSize)
    .clip(CircleShape)
    .zIndex(1f)

private val selectedNodeModifier = Modifier
    .size(nodeSize, nodeSize)
    .border(lineThickness, Color.Black)
    .zIndex( 2f)

@Composable
fun FieldHistoryView(
    currentNode: Node?,
    fieldHistory: FieldHistory,
    fieldHistoryViewData: FieldHistoryViewData,
    uiSettings: UiSettings,
    onChangeCurrentNode: () -> Unit
) {
    val requester = remember { FocusRequester() }
    Box(Modifier.size(300.dp, 200.dp)
        .horizontalScroll(rememberScrollState())
        .verticalScroll(rememberScrollState())
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
        Box(Modifier.size(fieldHistoryViewData.size)) {
            ConnectionsAndNodes(
                fieldHistory,
                fieldHistoryViewData.fieldHistoryElements,
                onChangeCurrentNode,
                uiSettings
            )
            CurrentNode(currentNode, fieldHistoryViewData)
        }
    }
    LaunchedEffect(fieldHistory) {
        requester.requestFocus()
    }
}

class FieldHistoryViewData(fieldHistory: FieldHistory) {
    val fieldHistoryElements: FieldHistoryElements = fieldHistory.getHistoryElements(mainBranchIsAlwaysStraight = true)

    val size: DpSize = DpSize(
        stepSize * fieldHistoryElements.size + nodeSize,
        stepSize * fieldHistoryElements.maxOf { yLine -> yLine.size } + nodeSize
    )

    val nodeToIndexMap: Map<Node, Pair<Int, Int>> by lazy {
        buildMap {
            for (xIndex in fieldHistoryElements.indices) {
                for ((yIndex, element) in fieldHistoryElements[xIndex].withIndex()) {
                    val node = (element as? NodeHistoryElement)?.node ?: continue
                    this[node] = xIndex to yIndex
                }
            }
        }
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
        val offsetX = stepSize * xIndex

        for ((yIndex, element) in fieldHistoryElements[xIndex].withIndex()) {
            val offsetY = stepSize * yIndex

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
                            Modifier.offset(
                                stepSize * (xIndex - 1) + nodeRadius,
                                offsetY - lineThickness / 2 + nodeRadius
                            ).then(horizontalLineModifier)
                        )
                    }

                    // Render node
                    Box(
                        Modifier
                            .offset(offsetX, offsetY)
                            .then(nodeModifier)
                            .background(color)
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
                            .offset(stepSize * xIndex - lineThickness / 2 + nodeRadius, offsetY - stepSize + nodeRadius)
                            .then(verticalLineModifier)
                    )
                }

                is EmptyHistoryElement -> {}
            }
        }
    }
}

@Composable
private fun CurrentNode(currentNode: Node?, fieldHistoryViewData: FieldHistoryViewData) {
    if (currentNode == null) return
    val (xIndex, yIndex) = fieldHistoryViewData.nodeToIndexMap.getValue(currentNode)

    Box(
        Modifier.offset(stepSize * xIndex, stepSize * yIndex).then(selectedNodeModifier)
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