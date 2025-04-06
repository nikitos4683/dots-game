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

@Composable
fun NewGameDialog(
    rules: Rules,
    onDismiss: () -> Unit,
    onConfirmation: (newGameRules: Rules) -> Unit,
) {
    val minDimension = 2
    val maxDimension = 48

    var width by remember { mutableStateOf(rules.width) }
    var height by remember { mutableStateOf(rules.height) }
    var captureByBorder by remember { mutableStateOf(rules.captureByBorder) }

    var initialPositionType by remember { mutableStateOf(EnumMode(expanded = false, selected = rules.initialPositionType)) }
    var baseMode by remember { mutableStateOf(EnumMode(expanded = false,selected = rules.baseMode)) }
    var suicideAllowed by remember { mutableStateOf(rules.suicideAllowed) }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.width(470.dp).wrapContentHeight()) {
            Column(modifier = Modifier.padding(20.dp)) {
                IntegerSlider("Width", width, minDimension, maxDimension) {
                    width = it
                }
                IntegerSlider("Height", height, minDimension, maxDimension) {
                    height = it
                }

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

