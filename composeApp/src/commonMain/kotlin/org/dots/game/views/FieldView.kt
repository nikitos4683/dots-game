package org.dots.game.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isPrimaryPressed
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
import org.dots.game.core.createPlacedState
import org.dots.game.core.getPositionsOfConnection
import org.dots.game.core.getSortedClosurePositions
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

private val drawHelperBases = false
private val connectionThickness = 2.dp
private val outOfBoundDrawRatio = dotRadiusRatio
private val baseDrawMode: PolygonDrawMode = PolygonDrawMode.OutlineAndFill
private val connectionDrawMode: ConnectionDrawMode = ConnectionDrawMode.Polygon(baseDrawMode)
private val drawDiagonalConnections: Boolean = true

sealed class ConnectionDrawMode {
    object None : ConnectionDrawMode()
    object Lines : ConnectionDrawMode()
    class Polygon(val polygonDrawMode: PolygonDrawMode) : ConnectionDrawMode()
}

enum class PolygonDrawMode {
    Outline,
    Fill,
    OutlineAndFill;

    val drawOutline: Boolean
        get() = this == Outline || this == OutlineAndFill
    val drawFill: Boolean
        get() = this == Fill || this == OutlineAndFill
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
                        val currentPlayer = moveMode.getMovePlayer() ?: field.getCurrentPlayer()
                        when (event.type) {
                            PointerEventType.Move -> {
                                pointerFieldPosition = event.toFieldPositionIfValid(field, currentPlayer, currentDensity)
                            }
                            PointerEventType.Press -> {
                                if (event.buttons.isPrimaryPressed) {
                                    val fieldPosition =
                                        event.toFieldPositionIfValid(field, currentPlayer, currentDensity)
                                    if (fieldPosition != null &&
                                        field.makeMoveUnsafe(fieldPosition, currentPlayer) != null
                                    ) {
                                        onMovePlaced(field.lastMove!!)
                                    }
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
        if (drawDiagonalConnections) {
            DiagonalConnections(currentMove, field, uiSettings)
        }
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

    field.unmakeAllMoves() // TODO: rewrite

    for (moveResult in movesSequence) {
        field.makeMove(moveResult.position, moveResult.player)

        val moveResultPosition = moveResult.position
        val color = uiSettings.toColor(moveResult.player)

        if (connectionDrawMode == ConnectionDrawMode.Lines) {
            StrongConnectionLines(field, moveResultPosition, color)
        } else if (connectionDrawMode is ConnectionDrawMode.Polygon) {
            val connections = field.getPositionsOfConnection(moveResultPosition)
            Polygon(
                connections,
                emptyList(),
                moveResult.player,
                field,
                connectionDrawMode.polygonDrawMode,
                uiSettings
            )
        }

        val dotOffset = moveResultPosition.toDpOffset()
        Box(Modifier
            .offset(dotOffset.x - dotRadius, dotOffset.y - dotRadius)
            .size(dotSize)
            .background(color, CircleShape)
        )

        for (base in moveResult.bases) {
            if (!base.isReal && !drawHelperBases) continue

            val (outerClosure, innerClosures) = base.getSortedClosurePositions(field)
            Polygon(outerClosure, innerClosures, base.player, field, baseDrawMode, uiSettings)
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
private fun DiagonalConnections(currentMove: MoveResult?, field: Field, uiSettings: UiSettings) {
    val localDensity = LocalDensity.current
    Canvas(Modifier.graphicsLayer()) {
        for (x in Field.OFFSET..field.width) {
            for (y in Field.OFFSET..field.height) {
                val position = Position(x, y)
                val state = with (field) {
                    position.getState()
                }

                if (!state.checkPlaced() || state.checkTerritory()) continue

                val player = state.getPlacedPlayer()
                val playerPlaced = player.createPlacedState()

                fun drawConnectionIfNeeded(end: Position, adjacentPrevPosition: Position, adjacentNextPosition: Position) {
                    with (field) {
                        if (connectionDrawMode is ConnectionDrawMode.Polygon && connectionDrawMode.polygonDrawMode.drawFill) {
                            if (adjacentPrevPosition.getState().checkActive(playerPlaced) ||
                                adjacentNextPosition.getState().checkActive(playerPlaced)
                            ) {
                                return
                            }
                        }

                        if (field.checkPositionWithinBounds(end) && end.getState().checkActive(playerPlaced))
                            drawLine(
                                uiSettings.toColor(player),
                                position.toOffset(localDensity),
                                end.toOffset(localDensity),
                                strokeWidth = connectionThickness.toPx(),
                                alpha = 0.3f
                            )
                    }
                }

                with (field) {
                    val adjacentCommonPosition = Position(x, y + 1)
                    drawConnectionIfNeeded(Position(x - 1, y + 1), adjacentCommonPosition, Position(x - 1, y))
                    drawConnectionIfNeeded(Position(x + 1, y + 1), Position(x + 1, y), adjacentCommonPosition)
                }
            }
        }
        currentMove
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
    outerClosure: List<Position>,
    innerClosures: List<List<Position>>,
    player: Player,
    field: Field,
    polygonDrawMode: PolygonDrawMode,
    uiSettings: UiSettings,
) {
    if (outerClosure.size <= 1) return

    var minX: Int = Int.MAX_VALUE
    var minY: Int = Int.MAX_VALUE
    var maxX: Int = Int.MIN_VALUE
    var maxY: Int = Int.MIN_VALUE

    var coercedMinX: Float = Float.MAX_VALUE
    var coercedMinY: Float = Float.MAX_VALUE
    var coercedMaxX: Float = Float.MIN_VALUE
    var coercedMaxY: Float = Float.MIN_VALUE

    var containsBound = false

    for (position in outerClosure) {
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

    val size = DpSize(
        xCount.let { if (it == 0) connectionThickness else cellSize * it },
        yCount.let { if (it == 0) connectionThickness else cellSize * it }
    )

    val width: Float
    val height: Float
    with(LocalDensity.current) {
        width = size.width.toPx()
        height = size.height.toPx()
    }

    fun createPath(positions: List<Position>): Path {
        return Path().apply {
            for ((index, position) in positions.withIndex()) {
                val xCoordinate = if (xCount == 0) 0f else (position.x - minX).toFloat() / xCount * width
                val yCoordinate = if (yCount == 0) 0f else (position.y - minY).toFloat() / yCount * height

                if (index == 0) {
                    moveTo(xCoordinate, yCoordinate)
                } else {
                    lineTo(xCoordinate, yCoordinate)
                    if (index == outerClosure.size - 1) {
                        close()
                    }
                }
            }
        }
    }

    val path = createPath(outerClosure)

    val resultPath = if (innerClosures.isEmpty()) {
        path
    } else {
        Path().apply {
            for ((index, innerClosure) in innerClosures.withIndex()) {
                op(if (index == 0) path else this, createPath(innerClosure), PathOperation.Difference)
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

    Box(
        Modifier
            .graphicsLayer()
            .offset(offsetGraphical.x, offsetGraphical.y)
            .size(size)
            .then(clipShape?.let { Modifier.clip(it) } ?: Modifier)
            .drawBehind {
                if (polygonDrawMode.drawFill) {
                    drawPath(resultPath, polygonColor.copy(alpha = baseAlpha))
                }
                if (polygonDrawMode.drawOutline) {
                    drawPath(resultPath, polygonColor, style = Stroke(width = connectionThickness.toPx()))
                }
            }
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

private fun Position.toOffset(density: Density): androidx.compose.ui.geometry.Offset {
    with (density) {
        return androidx.compose.ui.geometry.Offset((x - Field.OFFSET).toGraphical().toPx(), (y - Field.OFFSET).toGraphical().toPx())
    }
}

private fun PointerEvent.toFieldPositionIfValid(field: Field, currentPlayer: Player, currentDensity: Density): Position? {
    val offset = changes.first().position

    with (currentDensity) {
        val x = round((offset.x.toDp() - fieldPadding) / cellSize).toInt().takeIf { it >= 0 } ?: return null
        val y = round((offset.y.toDp() - fieldPadding) / cellSize).toInt().takeIf { it >= 0 } ?: return null

        return Position(x + Field.OFFSET, y + Field.OFFSET).takeIf {
            field.checkPositionWithinBounds(it) && field.checkValidMove(it, currentPlayer)
        }
    }
}

private fun Int.toGraphical(): Dp = cellSize * this + fieldPadding
private fun Float.toGraphical(): Dp = cellSize * this + fieldPadding