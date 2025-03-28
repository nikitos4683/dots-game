package org.dots.game.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import org.dots.game.core.Game
import org.dots.game.openOrLoadSgf
import org.dots.game.sgf.SgfDiagnostic
import org.dots.game.sgf.buildLineOffsets
import org.dots.game.sgf.toLineColumnDiagnostic

@Composable
fun OpenSgfDialog(
    onDismiss: () -> Unit,
    onConfirmation: (game: Game) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    var sgfPathOrContentTextFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    var previousInput: String? = null
    var diagnostics by remember { mutableStateOf<List<SgfDiagnostic>>(listOf()) }
    var fileName by remember { mutableStateOf<String?>(null) }
    var game by remember { mutableStateOf<Game?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.width(400.dp).wrapContentHeight()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Sgf Path or Content: ", Modifier.fillMaxWidth(0.3f))
                    TextField(
                        sgfPathOrContentTextFieldValue, {
                            sgfPathOrContentTextFieldValue = it
                            val text = it.text
                            if (text != previousInput) {
                                previousInput = text

                                coroutineScope.launch {
                                    diagnostics = buildList {
                                        val result = openOrLoadSgf(text) { diagnostic ->
                                            add(diagnostic)
                                        }
                                        fileName = result.first
                                        game = result.second
                                    }
                                }
                            }
                        },
                        singleLine = fileName != null,
                        maxLines = if (fileName == null) 5 else 1
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp).padding(vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val lineOffsets by lazy(LazyThreadSafetyMode.NONE) { sgfPathOrContentTextFieldValue.text.buildLineOffsets() }
                        items(diagnostics.size) { index ->
                            val diagnostic = diagnostics[index]
                            var cardModifier = Modifier.fillMaxWidth()
                            if (fileName == null && diagnostic.textSpan != null) {
                                cardModifier = cardModifier.then(Modifier.clickable(onClick = {
                                    val textSpan = diagnostic.textSpan
                                    val start = textSpan.start
                                    val end = textSpan.end
                                    sgfPathOrContentTextFieldValue = sgfPathOrContentTextFieldValue.copy(selection = TextRange(start,
                                        if (end == start) {
                                            if (end < sgfPathOrContentTextFieldValue.text.length - 1)
                                                end + 1
                                            else if (end > 0)
                                                end - 1
                                            else
                                                end
                                        } else {
                                            end
                                        }
                                    ))
                                }))
                            }
                            Card(
                                modifier = cardModifier,
                                elevation = 4.dp
                            ) {
                                Text(
                                    text = diagnostic.toLineColumnDiagnostic(lineOffsets).toString(),
                                    style = MaterialTheme.typography.body1,
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = { game?.let { onConfirmation(it) } },
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 10.dp),
                    enabled = game != null
                ) {
                    Text("Open game")
                }
            }
        }
    }
}
