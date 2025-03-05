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
import org.dots.game.isDesktop
import org.dots.game.readFile
import org.dots.game.sgf.SgfConverter
import org.dots.game.sgf.SgfParser

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
                    Text("Sgf${if (isDesktop) " Path" else ""}: ", Modifier.fillMaxWidth(0.2f))
                    TextField(sgfPathOrContent, {
                        sgfPathOrContent = it
                        diagnostics = buildList {
                            try {
                                val sgf = if (isDesktop) readFile(sgfPathOrContent) else sgfPathOrContent
                                val sgfParseTree = SgfParser.parse(sgf) { parseError ->
                                    add(parseError.toString())
                                }
                                val games = SgfConverter.convert(sgfParseTree) { convertError ->
                                    add(convertError.toString())
                                }
                                game = games.firstOrNull()
                                if (games.size != 1) {
                                    add("Only single game inside one SGF is supported.")
                                }
                            } catch (e: Exception) {
                                add("${e.message}")
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
