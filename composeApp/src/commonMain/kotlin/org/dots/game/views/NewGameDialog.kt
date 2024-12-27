package org.dots.game.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Card
import androidx.compose.material.Checkbox
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.dots.game.core.Field
import org.dots.game.core.InitialPosition
import org.dots.game.core.Rules
import kotlin.math.round

@Composable
fun NewGameDialog(
    onDismiss: () -> Unit,
    onConfirmation: (newGameRules: Rules) -> Unit,
) {
    val minWidth = 3
    val maxWidth = 48
    val minHeight = 3
    val maxHeight = 48
    val rules = Rules.Standard

    var width by remember { mutableStateOf(rules.width) }
    var height by remember { mutableStateOf(rules.height) }
    var captureByBorder by remember { mutableStateOf(rules.captureByBorder) }
    var captureEmptyBase by remember { mutableStateOf(rules.captureEmptyBase) }

    var initialPositionExpanded by remember { mutableStateOf(false) }
    val initialPositionItems = InitialPosition.entries
    var initialPositionSelected by remember { mutableStateOf(rules.initialPosition) }

    @Composable
    fun SetupDimension(isWidth: Boolean) {
        val minDimension: Int
        val maxDimension: Int
        if (isWidth) {
            minDimension = minWidth
            maxDimension = maxWidth
        } else {
            minDimension = minHeight
            maxDimension = maxHeight
        }

        val range = maxDimension - minDimension
        fun getDimension() = if (isWidth) width else height

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = if (isWidth) "Width" else "Height", Modifier.fillMaxWidth(0.5f))
            Slider(
                value = (getDimension() - minDimension).toFloat() / range,
                onValueChange = {
                    val newDimensionValue = round(it * range + minDimension).toInt()
                    if (isWidth)
                        width = newDimensionValue
                    else
                        height = newDimensionValue
                },
                steps = range - 1,
                modifier = Modifier.width(150.dp)
            )
            Text(getDimension().toString())
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.width(400.dp).wrapContentHeight()) {
            Column(modifier = Modifier.padding(20.dp)) {
                SetupDimension(isWidth = true)
                SetupDimension(isWidth = false)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Initial position: ", Modifier.fillMaxWidth(0.5f))
                    Box(Modifier.fillMaxWidth().background(Color.LightGray).clickable(onClick = { initialPositionExpanded = true })) {
                        Text(initialPositionSelected.toString(), Modifier.align(Alignment.Center))
                        DropdownMenu(
                            initialPositionExpanded,
                            onDismissRequest = { initialPositionExpanded = false },
                        ) {
                            initialPositionItems.forEachIndexed { _, initialPositionType ->
                                DropdownMenuItem(onClick = {
                                    initialPositionSelected = initialPositionType
                                    initialPositionExpanded = false
                                }) {
                                    Text(initialPositionType.toString())
                                }
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Capture by border", Modifier.fillMaxWidth(0.5f))
                    Checkbox(captureByBorder, onCheckedChange = { captureByBorder = it })
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Capture empty base", Modifier.fillMaxWidth(0.5f))
                    Checkbox(captureEmptyBase, onCheckedChange = { captureEmptyBase = it })
                }

                TextButton(
                    onClick = { onConfirmation(Rules(width, height, captureByBorder, captureEmptyBase, initialPositionSelected)) },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text("Create new game")
                }
            }
        }
    }
}