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
import androidx.compose.ui.draw.clip
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
import org.dots.game.core.Player
import org.dots.game.core.Position
import org.dots.game.core.getPositionsOfConnection
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
private val drawBaseOutline = true
private val connectionThickness = 2.dp
private val outOfBoundDrawRatio = dotRadiusRatio
private val connectionMode = ConnectionMode.Polygons

enum class ConnectionMode {
    None,
    Lines,
    Polygons,
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
    val movesSequence = field.moveSequence.toList()

    field.unmakeAllMoves()

    for (moveResult in movesSequence) {
        field.makeMove(moveResult.position, moveResult.player)

        val moveResultPosition = moveResult.position
        val color = uiSettings.toColor(moveResult.player)

        if (connectionMode == ConnectionMode.Lines) {
            StrongConnectionLines(field, moveResultPosition, color)
        } else if (connectionMode == ConnectionMode.Polygons) {
            val connections = field.getPositionsOfConnection(moveResultPosition)
            Polygon(connections, moveResult.player, field, uiSettings)
        }

        val dotOffset = moveResultPosition.toDpOffset()
        Box(Modifier
            .offset(dotOffset.x - dotRadius, dotOffset.y - dotRadius)
            .size(dotSize)
            .background(color, CircleShape)
        )

        for (base in moveResult.bases) {
            if (base.isEmpty && !drawEmptyBases) continue

            Polygon(base.closurePositions, base.player, field, uiSettings)
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
private fun StrongConnectionLines(
    field: Field,
    moveResultPosition: Position,
    color: Color
) {
    val connections = field.getStrongConnectionLinePositions(moveResultPosition)
    if (connections.isEmpty()) return

    val dotOffset = moveResultPosition.toDpOffset()
    for (connection in connections) {
        val isHorizontal = connection.x - moveResultPosition.x != 0

        val connectionX: Dp
        val connectionY: Dp
        val connectionWidth: Dp
        val connectionHeight: Dp

        if (isHorizontal) {
            val widthRatio = when (connection.x) {
                0 -> -outOfBoundDrawRatio
                field.realWidth - 1 -> outOfBoundDrawRatio
                else -> (connection.x - moveResultPosition.x).toFloat()
            }

            connectionX =
                (moveResultPosition.x + (if (widthRatio >= 0) 0.0f else widthRatio) - Field.OFFSET).toGraphical()
            connectionY = dotOffset.y - connectionThickness / 2
            connectionWidth = cellSize * abs(widthRatio)
            connectionHeight = connectionThickness
        } else {
            val heightRatio = when (connection.y) {
                0 -> -outOfBoundDrawRatio
                field.realHeight - 1 -> outOfBoundDrawRatio
                else -> (connection.y - moveResultPosition.y).toFloat()
            }

            connectionX = dotOffset.x - connectionThickness / 2
            connectionY =
                (moveResultPosition.y + (if (heightRatio >= 0) 0.0f else heightRatio) - Field.OFFSET).toGraphical()
            connectionWidth = connectionThickness
            connectionHeight = cellSize * abs(heightRatio)
        }

        Box(Modifier.offset(connectionX, connectionY).size(connectionWidth, connectionHeight).background(color))
    }
}

@Composable
private fun Polygon(
    positions: List<Position>,
    player: Player,
    field: Field,
    uiSettings: UiSettings
) {
    if (positions.size <= 1) return

    var minX: Int = Int.MAX_VALUE
    var minY: Int = Int.MAX_VALUE
    var maxX: Int = Int.MIN_VALUE
    var maxY: Int = Int.MIN_VALUE

    var coercedMinX: Float = Float.MAX_VALUE
    var coercedMinY: Float = Float.MAX_VALUE
    var coercedMaxX: Float = Float.MIN_VALUE
    var coercedMaxY: Float = Float.MIN_VALUE

    var containsBound = false

    for (position in positions) {
        val (x, y) = position
        if (x < minX) minX = x
        if (x > maxX) maxX = x
        if (y < minY) minY = y
        if (y > maxY) maxY = y

        val coercedX = when (x) {
            0 -> {
                containsBound = true
                1 - outOfBoundDrawRatio
            }

            field.width + 1 -> {
                containsBound = true
                field.width + 1 - (1 - outOfBoundDrawRatio)
            }

            else -> x.toFloat()
        }
        val coercedY = when (y) {
            0 -> {
                containsBound = true
                1 - outOfBoundDrawRatio
            }

            field.height + 1 -> {
                containsBound = true
                field.height + 1 - (1 - outOfBoundDrawRatio)
            }

            else -> y.toFloat()
        }

        if (coercedX < coercedMinX) coercedMinX = coercedX
        if (coercedX > coercedMaxX) coercedMaxX = coercedX
        if (coercedY < coercedMinY) coercedMinY = coercedY
        if (coercedY > coercedMaxY) coercedMaxY = coercedY
    }

    val xCount = maxX - minX
    val yCount = maxY - minY

    val polygonShape = GenericShape { size, _ ->
        for ((index, position) in positions.withIndex()) {
            val x = if (xCount == 0) 0f else (position.x - minX).toFloat() / xCount * size.width
            val y = if (yCount == 0) 0f else (position.y - minY).toFloat() / yCount * size.height
            if (index == 0) {
                moveTo(x, y)
            } else {
                lineTo(x, y)
            }
        }
    }

    val clipShape = if (containsBound) {
        GenericShape { size, _ ->
            val xMinClip = (coercedMinX - minX) / xCount * size.width
            val yMinClip = (coercedMinY - minY) / yCount * size.height
            val xMaxClip = (1 - (maxX - coercedMaxX) / xCount) * size.width
            val yMaxClip = (1 - (maxY - coercedMaxY) / yCount) * size.height
            moveTo(xMinClip, yMinClip)
            lineTo(xMaxClip, yMinClip)
            lineTo(xMaxClip, yMaxClip)
            lineTo(xMinClip, yMaxClip)
        }
    } else {
        null
    }

    val offsetGraphical = Position(minX, minY).toDpOffset()
    val polygonColor = uiSettings.toColor(player)

    val borderModifier = if (drawBaseOutline || connectionMode == ConnectionMode.Lines) {
        Modifier.border(connectionThickness, polygonColor, polygonShape)
    } else {
        Modifier
    }

    Box(
        Modifier
            .offset(offsetGraphical.x, offsetGraphical.y)
            .size(
                xCount.let { if (it == 0) connectionThickness else cellSize * it },
                yCount.let { if (it == 0) connectionThickness else cellSize * it }
            )
            .then(clipShape?.let { Modifier.clip(it) } ?: Modifier)
            .background(polygonColor.copy(alpha = baseAlpha), polygonShape)
            .then(borderModifier)
    )
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