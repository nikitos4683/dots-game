package org.dots.game.views

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
import kotlin.math.round

private val borderPaddingRatio = 1.0f
private val textPaddingRatio = 0.2f
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

private val linesThickness = 0.75.dp

@Composable
fun FieldView(currentMove: MoveResult?, moveMode: MoveMode, fieldViewData: FieldViewData, uiSettings: UiSettings, onMovePlaced: (MoveResult) -> Unit) {
    val field = fieldViewData.field
    val currentDensity = LocalDensity.current
    Box(Modifier.size(fieldViewData.fieldSize).pointerInput(moveMode, field) {
        detectTapGestures(
            onPress = { tapOffset ->
                if (handleTap(tapOffset, moveMode.getMovePlayer(), field, currentDensity)) {
                    onMovePlaced(field.lastMove!!)
                }
            },
        )
    }) {
        Grid(field)
        Moves(currentMove, field, uiSettings)
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
        val dotOffset = moveResult.position.toDpOffset(field)
        Box(Modifier
            .offset(dotOffset.x - dotRadius, dotOffset.y - dotRadius)
            .size(dotSize)
            .background(uiSettings.toColor(moveResult.player), CircleShape)
        )

        for (base in moveResult.bases) {
            if (base.isEmpty && !drawEmptyBases) continue

            var minX: Int = Int.MAX_VALUE
            var minY: Int = Int.MAX_VALUE
            var maxX: Int = Int.MIN_VALUE
            var maxY: Int = Int.MIN_VALUE

            val denormalizedPositions = buildList {
                for (position in base.closurePositions) {
                    val denormalizedPosition = with(field) { position.denormalize() }.also { add(it) }

                    if (denormalizedPosition.x < minX) minX = denormalizedPosition.x
                    if (denormalizedPosition.x > maxX) maxX = denormalizedPosition.x
                    if (denormalizedPosition.y < minY) minY = denormalizedPosition.y
                    if (denormalizedPosition.y > maxY) maxY = denormalizedPosition.y
                }
            }

            val xCount = (maxX - minX).toFloat()
            val yCount = (maxY - minY).toFloat()

            val polygonShape = GenericShape { size, _ ->
                for ((index, position) in denormalizedPositions.withIndex()) {
                    val x = (position.x - minX) / xCount * size.width
                    val y = (position.y - minY) / yCount * size.height
                    if (index == 0) {
                        moveTo(x, y)
                    } else {
                        lineTo(x, y)
                    }
                }
            }

            val xGraphical = minX.toGraphical()
            val yGraphical = minY.toGraphical()

            Box(
                Modifier
                    .offset(xGraphical, yGraphical)
                    .size(maxX.toGraphical() - xGraphical, maxY.toGraphical() - yGraphical)
                    .background(uiSettings.toColor(moveResult.player).copy(alpha = baseAlpha), polygonShape)
            )
        }
    }

    currentMove?.let {
        val offset = it.position.toDpOffset(field)
        Box(Modifier
            .offset(offset.x - lastMoveRadius, offset.y - lastMoveRadius)
            .size(lastMoveDotSize)
            .background(lastMoveColor, CircleShape)
        )
    }
}

private fun Position.toDpOffset(field: Field): DpOffset {
    val denormalized = with (field) {
        this@toDpOffset.denormalize()
    }
    return DpOffset( denormalized.x.toGraphical(),  denormalized.y.toGraphical())
}

private fun Int.toGraphical(): Dp = cellSize * this + fieldPadding

private fun handleTap(tapOffset: Offset, currentPlayer: Player?, field: Field, currentDensity: Density): Boolean {
    with (currentDensity) {
        val x = round((tapOffset.x.toDp() - fieldPadding) / cellSize).toInt()
        val y = round((tapOffset.y.toDp() - fieldPadding) / cellSize).toInt()

        return field.makeMove(x, y, field.getCurrentPlayer(currentPlayer)) != null
    }
}