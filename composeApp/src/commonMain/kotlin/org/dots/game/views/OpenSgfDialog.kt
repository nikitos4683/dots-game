package org.dots.game.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.dots.game.core.Game
import org.dots.game.openOrLoadSgf

@Composable
fun OpenSgfDialog(
    onDismiss: () -> Unit,
    onConfirmation: (game: Game) -> Unit,
) {
    var sgfPathOrContent by remember { mutableStateOf("") }
    var diagnostics by remember { mutableStateOf<List<String>>(listOf()) }
    var game by remember { mutableStateOf<Game?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.width(400.dp).wrapContentHeight()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Sgf Path or Content: ", Modifier.fillMaxWidth(0.3f))
                    TextField(sgfPathOrContent, {
                        sgfPathOrContent = it
                        diagnostics = buildList {
                            game = openOrLoadSgf(sgfPathOrContent) { error ->
                                add(error.toString())
                            }
                        }
                    }, Modifier.height(60.dp), singleLine = true)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp).padding(vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(diagnostics.size) { index ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = 4.dp
                            ) {
                                SelectionContainer {
                                    Text(
                                        text = diagnostics[index],
                                        style = MaterialTheme.typography.body1,
                                        modifier = Modifier
                                            .padding(16.dp)
                                            .fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }

                TextButton(
                    onClick = { game?.let { onConfirmation(it) } },
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 10.dp),
                ) {
                    Text("Open game")
                }
            }
        }
    }
}
