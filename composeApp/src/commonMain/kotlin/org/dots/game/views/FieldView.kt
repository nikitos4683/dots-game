package org.dots.game.views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.dots.game.UiSettings
import org.dots.game.core.Field
import org.dots.game.core.MoveMode
import org.dots.game.core.MoveResult
import org.dots.game.core.Position
import org.dots.game.core.getStrongConnectionLinePositions
import kotlin.math.abs
import kotlin.math.round

private val borderPaddingRatio = 2.0f
private val textPaddingRatio = 1.0f
private val cellSize = 22.dp
private val linesColor = Color.hsv(0.0f, 0.0f, 0.25f)

private val fieldColor = Color.hsv(0.0f, 0.0f, 1.0f)
private val dotRadiusRatio = 0.28f
private val lastMoveRadiusRatio = 0.14f
private val baseAlpha = 0.5f

private val lastMoveColor = Color.White

private val fieldPadding = cellSize * borderPaddingRatio
private val textPadding = cellSize * textPaddingRatio
private val dotRadius = cellSize * dotRadiusRatio
private val dotSize = DpSize(dotRadius * 2, dotRadius * 2)
private val lastMoveRadius = cellSize * lastMoveRadiusRatio
private val lastMoveDotSize = DpSize(lastMoveRadius * 2, lastMoveRadius * 2)

private val drawEmptyBases = false
private val drawBaseOutline = false
private val connectionThickness = 2.dp
private val strongConnectionMode = StrongConnectionMode.Lines

enum class StrongConnectionMode {
    None,
    Lines,
}

private val linesThickness = 0.75.dp

@Composable
fun FieldView(currentMove: MoveResult?, moveMode: MoveMode, fieldViewData: FieldViewData, uiSettings: UiSettings, onMovePlaced: (MoveResult) -> Unit) {
    val field = fieldViewData.field
    val currentDensity = LocalDensity.current
    var pointerFieldPosition: Position? by remember { mutableStateOf(null) }

    Box(
        Modifier
            .size(fieldViewData.fieldSize)
            .pointerInput(moveMode, field) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        when (event.type) {
                            PointerEventType.Move -> {
                                pointerFieldPosition = event.toFieldPositionIfFree(field, currentDensity)
                            }
                            PointerEventType.Press -> {
                                val fieldPosition = event.toFieldPositionIfFree(field, currentDensity)
                                if (fieldPosition != null &&
                                    field.makeMoveUnsafe(fieldPosition, moveMode.getMovePlayer() ?: field.getCurrentPlayer()) != null
                                ) {
                                    onMovePlaced(field.lastMove!!)
                                }
                            }
                            PointerEventType.Exit -> {
                                pointerFieldPosition = null
                            }
                        }
                    }
                }
            }
    ) {
        Grid(field)
        Moves(currentMove, field, uiSettings)
        Pointer(pointerFieldPosition, moveMode, field, uiSettings)
    }
}

class FieldViewData(val field: Field) {
    val fieldSize = DpSize(
        cellSize * (field.width - 1) + fieldPadding * 2,
        cellSize * (field.height - 1) + fieldPadding * 2,
    )
}

@Composable
private fun Grid(field: Field) {
    val textMeasurer = rememberTextMeasurer()
    val verticalLinesLength =  cellSize * (field.height - 1)
    val horizontalLinesLength = cellSize * (field.width - 1)

    Box(Modifier
        .fillMaxSize()
        .background(fieldColor)
    )

    for (x in 0 until field.width) {
        val xGraphical = x.toGraphical()

        val coordinate = (x + 1).toString()
        val textLayoutResult = textMeasurer.measure(coordinate)

        with ( LocalDensity.current) {
            Text(
                coordinate,
                Modifier.offset(
                    xGraphical - textLayoutResult.size.width.toDp() / 2,
                    fieldPadding - textPadding - textLayoutResult.size.height.toDp()
                ),
                linesColor
            )
        }

        Box(Modifier
            .offset(xGraphical - linesThickness / 2, fieldPadding)
            .size(linesThickness, verticalLinesLength)
            .background(linesColor)
        )
    }

    for (y in 0 until field.height) {
        val yGraphical = y.toGraphical()

        val coordinate = (y + 1).toString()
        val textLayoutResult = textMeasurer.measure(coordinate)

        with ( LocalDensity.current) {
            Text(
                coordinate,
                Modifier.offset(
                    fieldPadding - textPadding - textLayoutResult.size.width.toDp(),
                    yGraphical - textLayoutResult.size.height.toDp() / 2
                ),
                linesColor,
            )
        }

        Box(Modifier
            .offset(fieldPadding, yGraphical - linesThickness / 2)
            .size(horizontalLinesLength, linesThickness)
            .background(linesColor)
        )
    }
}

@Composable
private fun Moves(currentMove: MoveResult?, field: Field, uiSettings: UiSettings) {
    for (moveResult in field.moveSequence) {
        val moveResultPosition = moveResult.position
        val dotOffset = moveResultPosition.toDpOffset()
        val color = uiSettings.toColor(moveResult.player)

        if (strongConnectionMode == StrongConnectionMode.Lines) {
            val connections = field.getStrongConnectionLinePositions(moveResultPosition)
            for (connection in connections) {
                val isHorizontal = connection.x - moveResultPosition.x != 0

                val connectionX: Dp
                val connectionY: Dp
                val connectionWidth: Dp
                val connectionHeight: Dp

                if (isHorizontal) {
                    val widthRatio = when (connection.x) {
                        0 -> -0.5f
                        field.realWidth - 1 -> 0.5f
                        else -> (connection.x - moveResultPosition.x).toFloat()
                    }

                    connectionX = (moveResultPosition.x + (if (widthRatio >= 0) 0.0f else widthRatio) - Field.OFFSET).toGraphical()
                    connectionY = dotOffset.y - connectionThickness / 2
                    connectionWidth = cellSize * abs(widthRatio)
                    connectionHeight = connectionThickness

                } else {
                    val heightRaio = when (connection.y) {
                        0 -> -0.5f
                        field.realHeight - 1 -> 0.5f
                        else -> (connection.y - moveResultPosition.y).toFloat()
                    }

                    connectionX = dotOffset.x - connectionThickness / 2
                    connectionY = (moveResultPosition.y + (if (heightRaio >= 0) 0.0f else heightRaio) - Field.OFFSET).toGraphical()
                    connectionWidth = connectionThickness
                    connectionHeight = cellSize * abs(heightRaio)
                }

                Box(Modifier.offset(connectionX,connectionY).size(connectionWidth, connectionHeight).background(color))
            }
        }

        Box(Modifier
            .offset(dotOffset.x - dotRadius, dotOffset.y - dotRadius)
            .size(dotSize)
            .background(color, CircleShape)
        )

        for (base in moveResult.bases) {
            if (base.isEmpty && !drawEmptyBases) continue

            var minX: Int = Int.MAX_VALUE
            var minY: Int = Int.MAX_VALUE
            var maxX: Int = Int.MIN_VALUE
            var maxY: Int = Int.MIN_VALUE

            // Drop out-of-bounds drawing
            fun Position.coerceInRealSize() = Position(
                x.coerceIn(Field.OFFSET, field.width),
                y.coerceIn(Field.OFFSET, field.height),
            )

            for (position in base.closurePositions) {
                val (x, y) = position.coerceInRealSize()
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
            }

            val xCount = (maxX - minX).toFloat()
            val yCount = (maxY - minY).toFloat()

            val polygonShape = GenericShape { size, _ ->
                for ((index, position) in base.closurePositions.withIndex()) {
                    val coercedPosition = position.coerceInRealSize()
                    val x = (coercedPosition.x - minX) / xCount * size.width
                    val y = (coercedPosition.y - minY) / yCount * size.height
                    if (index == 0) {
                        moveTo(x, y)
                    } else {
                        lineTo(x, y)
                    }
                }
            }

            val offsetGraphical = Position(minX, minY).toDpOffset()
            val baseColor = uiSettings.toColor(base.player)

            val borderModifier = if (drawBaseOutline || strongConnectionMode == StrongConnectionMode.Lines) {
                Modifier.border(connectionThickness, baseColor, polygonShape)
            } else {
                Modifier
            }

            Box(
                Modifier
                    .offset(offsetGraphical.x, offsetGraphical.y)
                    .size(cellSize * (maxX - minX), cellSize * (maxY - minY))
                    .background(baseColor.copy(alpha = baseAlpha), polygonShape)
                    .then(borderModifier)
            )
        }
    }

    currentMove?.let {
        val dpOffset = it.position.toDpOffset()
        Box(Modifier
            .offset(dpOffset.x - lastMoveRadius, dpOffset.y - lastMoveRadius)
            .size(lastMoveDotSize)
            .background(lastMoveColor, CircleShape)
        )
    }
}

@Composable
private fun Pointer(position: Position?, moveMode: MoveMode, field: Field, uiSettings: UiSettings) {
    if (position == null) return

    val dpOffset = position.toDpOffset()
    val currentPlayer = moveMode.getMovePlayer() ?: field.getCurrentPlayer()
    Box(Modifier
        .offset(dpOffset.x - dotRadius, dpOffset.y - dotRadius)
        .size(dotSize)
        .background(uiSettings.toColor(currentPlayer).copy(alpha = 0.5f), CircleShape)
    )
}

private fun Position.toDpOffset(): DpOffset {
    return DpOffset((x - Field.OFFSET).toGraphical(), (y - Field.OFFSET).toGraphical())
}

private fun PointerEvent.toFieldPositionIfFree(field: Field, currentDensity: Density): Position? {
    val offset = changes.first().position

    with (currentDensity) {
        val x = round((offset.x.toDp() - fieldPadding) / cellSize).toInt().takeIf { it >= 0 } ?: return null
        val y = round((offset.y.toDp() - fieldPadding) / cellSize).toInt().takeIf { it >= 0 } ?: return null

        return Position(x + Field.OFFSET, y + Field.OFFSET).takeIf {
            field.checkPositionWithinBounds(it) && field.checkValidMove(it)
        }
    }
}

private fun Int.toGraphical(): Dp = cellSize * this + fieldPadding
private fun Float.toGraphical(): Dp = cellSize * this + fieldPadding