package org.dots.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.dots.game.core.*
import org.dots.game.views.FieldHistoryView
import org.dots.game.views.FieldView
import org.dots.game.views.NewGameDialog
import org.jetbrains.compose.ui.tooling.preview.Preview

private var start = true
private val startRules = Rules(10, 10, captureEmptyBase = true, captureByBorder = true)

private var moveMode = MoveMode.Next
private lateinit var field: Field
private lateinit var fieldHistory: FieldHistory
private lateinit var fieldView: FieldView
private lateinit var fieldHistoryView: FieldHistoryView

fun getMoveColor(): Player? {
    return when (moveMode) {
        MoveMode.Next -> null
        MoveMode.First -> Player.First
        MoveMode.Second -> Player.Second
    }
}

@Composable
@Preview
fun App() {
    val uiSettings = UiSettings.Standard
    val textMeasurer = rememberTextMeasurer()

    MaterialTheme {
        var lastMove: MoveResult? by remember { mutableStateOf<MoveResult?>(null) }
        var currentNode by remember { mutableStateOf<Node?>(null) }
        var player1Score by remember { mutableStateOf("0") }
        var player2Score by remember { mutableStateOf("0") }
        var fieldHistorySize by remember { mutableStateOf(DpSize(0.dp, 0.dp)) }
        val showNewGameDialog = remember { mutableStateOf(false) }

        fun updateFieldAndHistory() {
            player1Score = field.player1Score.toString()
            player2Score = field.player2Score.toString()
            lastMove = field.lastMove
            currentNode = fieldHistory.currentNode
            fieldHistorySize = fieldHistoryView.calculateSize()
        }

        fun initializeFieldAndHistory(rules: Rules) {
            field = Field(rules)
            fieldHistory = FieldHistory(field)
            fieldView = FieldView(field, textMeasurer, uiSettings)
            fieldHistoryView = FieldHistoryView(fieldHistory, textMeasurer, uiSettings)

            updateFieldAndHistory()
        }

        if (showNewGameDialog.value) {
            NewGameDialog(
                onDismiss = {
                    showNewGameDialog.value = false
                },
                onConfirmation = { rules ->
                    showNewGameDialog.value = false
                    initializeFieldAndHistory(rules)
                }
            )
        } else if (start) {
            initializeFieldAndHistory(startRules)
            start = false
        }

        Row {
            Column(Modifier.width(with(LocalDensity.current) { fieldView.fieldGraphicsWidth.toDp() })) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Text(player1Score, color = uiSettings.playerFirstColor)
                    Text(" : ")
                    Text(player2Score, color = uiSettings.playerSecondColor)
                }
                Row {
                    Canvas(
                        Modifier.fillMaxSize().pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = { tapOffset ->
                                        if (fieldView.handleTap(tapOffset, getMoveColor())) {
                                            fieldHistory.add(field.lastMove!!)
                                            updateFieldAndHistory()
                                        }
                                    },
                                )
                            },
                        contentDescription = "Field"
                    ) {
                        lastMove
                        currentNode
                        fieldView.draw(this)
                    }
                }
            }
            Column {
                Button(onClick = { showNewGameDialog.value = true }) {
                    Text("New Game")
                }
                Button(onClick = { initializeFieldAndHistory(field.rules) }) {
                    Text("Reset")
                }
                Canvas(
                    Modifier.size(fieldHistorySize).focusable().onKeyEvent { keyEvent ->
                        if (fieldHistoryView.handleKey(keyEvent)) {
                            updateFieldAndHistory()
                            true
                        } else {
                            false
                        }
                    }.pointerInput(Unit) {
                        detectTapGestures(
                            onPress = { tapOffset ->
                                if (fieldHistoryView.handleTap(tapOffset)) {
                                    updateFieldAndHistory()
                                }
                            }
                        )
                    },
                    contentDescription = "Field Tree"
                ) {
                    lastMove
                    currentNode
                    fieldHistoryView.draw(this)
                }
            }
        }
    }
}
