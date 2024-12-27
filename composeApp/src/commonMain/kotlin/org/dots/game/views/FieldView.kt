package org.dots.game.views

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import org.dots.game.UiSettings
import org.dots.game.core.Field
import org.dots.game.core.Player
import org.dots.game.core.Position
import kotlin.math.round

class FieldView(val field: Field, val textMeasurer: TextMeasurer, val uiSettings: UiSettings) {
    val borderPaddingRatio = 1.0f
    val textPaddingRatio = 0.2f
    val cellSize = 30.0f
    val linesColor = Color.hsv(0.0f, 0.0f, 0.25f)

    val fieldColor = Color.White
    val dotRadiusRatio = 0.27f
    val baseAlpha = 0.5f

    val lastMoveColor = Color.White
    val lastMoveRadiusRatio = 0.15f

    val fieldPadding = borderPaddingRatio * cellSize
    val textPadding = textPaddingRatio * cellSize
    val dotRadius = dotRadiusRatio * cellSize
    val lastMoveRadius = lastMoveRadiusRatio * cellSize

    val fieldGraphicsWidth = ((field.width - 1) * cellSize + fieldPadding * 2).toFloat()
    val fieldGraphicsHeight = ((field.height - 1) * cellSize + fieldPadding * 2).toFloat()
    val fieldGraphicsSize = Size(fieldGraphicsWidth, fieldGraphicsHeight)

    val drawEmptyBases = false

    fun draw(drawScope: DrawScope) {
        drawScope.drawField()
        drawScope.drawMoves()
    }

    private fun DrawScope.drawField() {
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

    private fun DrawScope.drawMoves() {
        val movesSequence = field.moveSequence
        for (moveResult in movesSequence) {
            drawCircle(uiSettings.toColor(moveResult.player), dotRadius, moveResult.position.toOffset())

            for (base in moveResult.bases) {
                if (base.isEmpty && !drawEmptyBases) continue

                val path = Path()
                for ((index, position) in base.closurePositions.withIndex()) {
                    val offset = position.toOffset()
                    if (index == 0) {
                        path.moveTo(offset.x, offset.y)
                    } else {
                        path.lineTo(offset.x, offset.y)
                    }
                }

                /*val style: DrawStyle
                    if (base.nonCapturing) {
                        style = Stroke(cap = Sroke)
                    } else {
                        style = Fill
                    }*/

                drawPath(path, uiSettings.toColor(base.player), baseAlpha)
            }
        }

        movesSequence.lastOrNull()?.let {
            drawCircle(lastMoveColor, lastMoveRadius, it.position.toOffset())
        }
    }

    private fun Position.toOffset() = with (field) {
        val denormalized = this@toOffset.denormalize()
        Offset(denormalized.x * cellSize + fieldPadding, denormalized.y * cellSize + fieldPadding)
    }

    fun handleTap(tapOffset: Offset, currentPlayer: Player?): Boolean {
        val x = round((tapOffset.x - fieldPadding) / cellSize).toInt()
        val y = round((tapOffset.y - fieldPadding) / cellSize).toInt()

        return field.makeMove(x, y, field.getCurrentPlayer(currentPlayer)) != null
    }
}