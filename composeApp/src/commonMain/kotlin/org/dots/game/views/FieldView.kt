package org.dots.game.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import org.dots.game.UiSettings
import org.dots.game.core.Field
import org.dots.game.core.MoveMode
import org.dots.game.core.MoveResult
import org.dots.game.core.Player
import org.dots.game.core.Position
import kotlin.math.round

private val borderPaddingRatio = 1.0f
private val textPaddingRatio = 0.2f
private val cellSize = 30.0f
private val linesColor = Color.hsv(0.0f, 0.0f, 0.25f)

private val fieldColor = Color.White
private val dotRadiusRatio = 0.27f
private val baseAlpha = 0.5f

private val lastMoveColor = Color.White
private val lastMoveRadiusRatio = 0.15f

private val fieldPadding = borderPaddingRatio * cellSize
private val textPadding = textPaddingRatio * cellSize
private val dotRadius = dotRadiusRatio * cellSize
private val lastMoveRadius = lastMoveRadiusRatio * cellSize

private val drawEmptyBases = false

@Composable
fun FieldView(currentMove: MoveResult?, moveMode: MoveMode, field: Field, uiSettings: UiSettings, onMovePlaced: (MoveResult) -> Unit) {
    val textMeasurer = rememberTextMeasurer()
    Canvas(
        Modifier.fillMaxSize().pointerInput(moveMode, field) {
            detectTapGestures(
                onPress = { tapOffset ->
                    if (handleTap(tapOffset, moveMode.getMovePlayer(), field)) {
                        onMovePlaced(field.lastMove!!)
                    }
                },
            )
        },
        contentDescription = "Field"
    ) {
        drawField(field, textMeasurer)
        drawMoves(field, uiSettings)
        drawLastMove(currentMove, field)
    }
}

private fun DrawScope.drawField(field: Field, textMeasurer: TextMeasurer) {
    val (fieldGraphicsWidth, fieldGraphicsHeight) = calculateFieldSize(field)
    val fieldGraphicsSize = Size(fieldGraphicsWidth, fieldGraphicsHeight)

    drawRect(fieldColor, size = fieldGraphicsSize)

    for (x in 0 until field.width) {
        val xGraphical = (x * cellSize).toFloat() + fieldPadding

        val coordinate = (x + 1).toString()
        val textLayoutResult = textMeasurer.measure(coordinate)
        drawText(textLayoutResult, linesColor, Offset(xGraphical - textLayoutResult.size.width / 2, fieldPadding - textPadding - textLayoutResult.size.height))

        drawLine(
            linesColor,
            Offset(xGraphical, fieldPadding),
            Offset(xGraphical, fieldGraphicsHeight - fieldPadding)
        )
    }

    for (y in 0 until field.height) {
        val yGraphical = (y * cellSize).toFloat() + fieldPadding

        val coordinate = (y + 1).toString()
        val textLayoutResult = textMeasurer.measure(coordinate)
        drawText(textLayoutResult, linesColor, Offset(fieldPadding - textPadding - textLayoutResult.size.width, yGraphical - textLayoutResult.size.height / 2))

        drawLine(
            linesColor,
            Offset(fieldPadding, yGraphical),
            Offset(fieldGraphicsWidth - fieldPadding, yGraphical)
        )
    }
}

private fun DrawScope.drawMoves(field: Field, uiSettings: UiSettings) {
    val movesSequence = field.moveSequence
    for (moveResult in movesSequence) {
        drawCircle(uiSettings.toColor(moveResult.player), dotRadius, moveResult.position.toOffset(field))

        for (base in moveResult.bases) {
            if (base.isEmpty && !drawEmptyBases) continue

            val path = Path()
            for ((index, position) in base.closurePositions.withIndex()) {
                val offset = position.toOffset(field)
                if (index == 0) {
                    path.moveTo(offset.x, offset.y)
                } else {
                    path.lineTo(offset.x, offset.y)
                }
            }

            drawPath(path, uiSettings.toColor(base.player), baseAlpha)
        }
    }
}

private fun DrawScope.drawLastMove(currentMove: MoveResult?, field: Field) {
    currentMove?.let {
        drawCircle(lastMoveColor, lastMoveRadius, it.position.toOffset(field))
    }
}

private fun Position.toOffset(field: Field) = with (field) {
    val denormalized = this@toOffset.denormalize()
    Offset(denormalized.x * cellSize + fieldPadding, denormalized.y * cellSize + fieldPadding)
}

private fun handleTap(tapOffset: Offset, currentPlayer: Player?, field: Field): Boolean {
    val x = round((tapOffset.x - fieldPadding) / cellSize).toInt()
    val y = round((tapOffset.y - fieldPadding) / cellSize).toInt()

    return field.makeMove(x, y, field.getCurrentPlayer(currentPlayer)) != null
}

fun calculateFieldSize(field: Field): Size {
    return Size(
        ((field.width - 1) * cellSize + fieldPadding * 2).toFloat(),
        ((field.height - 1) * cellSize + fieldPadding * 2).toFloat()
    )
}