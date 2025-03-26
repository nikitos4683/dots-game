package org.dots.game.views

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.dots.game.core.InitialPositionType
import org.dots.game.core.Rules
import org.dots.game.core.generateDefaultInitialPositions
import org.dots.game.splitByUppercase
import kotlin.math.round

private const val textFraction = 0.4f

@Composable
fun NewGameDialog(
    rules: Rules,
    onDismiss: () -> Unit,
    onConfirmation: (newGameRules: Rules) -> Unit,
) {
    val minWidth = 3
    val maxWidth = 48
    val minHeight = 3
    val maxHeight = 48

    var width by remember { mutableStateOf(rules.width) }
    var height by remember { mutableStateOf(rules.height) }
    var captureByBorder by remember { mutableStateOf(rules.captureByBorder) }

    var initialPositionType by remember { mutableStateOf(EnumMode(expanded = false, selected = rules.initialPositionType)) }
    var baseMode by remember { mutableStateOf(EnumMode(expanded = false,selected = rules.baseMode)) }
    var suicideAllowed by remember { mutableStateOf(rules.suicideAllowed) }

    @Composable
    fun Dimension(isWidth: Boolean) {
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
            Text(text = if (isWidth) "Width" else "Height", Modifier.fillMaxWidth(textFraction))
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
        Card(modifier = Modifier.width(470.dp).wrapContentHeight()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Dimension(isWidth = true)
                Dimension(isWidth = false)

                Mode(initialPositionType, ignoredEntries = setOf(InitialPositionType.Custom)) {
                    initialPositionType = it
                }
                Mode(baseMode) {
                    baseMode = it
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Capture by border", Modifier.fillMaxWidth(textFraction))
                    Checkbox(captureByBorder, onCheckedChange = { captureByBorder = it })
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Suicide allowed", Modifier.fillMaxWidth(textFraction))
                    Checkbox(suicideAllowed, onCheckedChange = { suicideAllowed = it })
                }

                Button(
                    onClick = {
                        val initialMoves = initialPositionType.selected.generateDefaultInitialPositions(width, height)!!
                        onConfirmation(Rules(width, height, captureByBorder, baseMode.selected, suicideAllowed, initialMoves))
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text("Create new game")
                }
            }
        }
    }
}

private data class EnumMode<E: Enum<E>>(val expanded: Boolean, val selected: E)

@Composable
private inline fun <reified E : Enum<E>> Mode(enumMode: EnumMode<E>, ignoredEntries: Set<E> = emptySet(), crossinline onChange: (newMode: EnumMode<E>) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 10.dp)) {
        Text("${splitByUppercase(E::class.simpleName!!)} ", Modifier.fillMaxWidth(textFraction))
        Column(Modifier.fillMaxWidth().height(30.dp)
            .border(1.dp, Color.hsv(0f, 0f, 0.4f))
            .clickable(onClick = { onChange(enumMode.copy(expanded = true)) }),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(splitByUppercase(enumMode.selected.toString()), Modifier.align(Alignment.CenterHorizontally))
            DropdownMenu(
                enumMode.expanded,
                onDismissRequest = { onChange(enumMode.copy(expanded = false)) },
            ) {
                enumValues<E>().filterNot { ignoredEntries.contains(it) } .forEach { entry ->
                    DropdownMenuItem(onClick = { onChange(enumMode.copy(expanded = false, selected = entry)) }) {
                        Text(splitByUppercase(entry.toString()))
                    }
                }
            }
        }
    }
}

