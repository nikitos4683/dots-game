package org.dots.game.sgf

import org.dots.game.core.AppInfo
import org.dots.game.core.Game
import org.dots.game.core.GameTreeNode
import org.dots.game.core.MoveInfo
import org.dots.game.core.Player
import org.dots.game.core.PositionPlayer
import org.dots.game.core.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SgfConverterTests {
    @Test
    fun gameInfo() {
        val (gameInfo, rules) = parseAndConvert(
            "(;GM[40]FF[4]CA[UTF-8]SZ[17:21]RU[russian]GN[Test Game]PB[Player1]BR[256]BT[Player1's Team]PW[Player2]WR[512]WT[Player2's Team]KM[0.5]DT[2025-01-19]GC[A game for SGF parser testing]C[Comment to node]PC[Amsterdam, Netherlands]EV[Test event]ON[Empty]AN[Ivan Kochurkin]CP[@]SO[https://zagram.org/eidokropki/index.html]TM[300]OT[0+25]AP[https\\://zagram.org/eidokropki/index.html:1])",
            ).single()
        with (gameInfo) {
            assertEquals(17, rules.width)
            assertEquals(21, rules.height)
            assertEquals("Test Game", gameName)
            assertEquals("Player1", player1Name)
            assertEquals(256.0, player1Rating)
            assertEquals("Player1's Team", player1Team)
            assertEquals("Player2", player2Name)
            assertEquals(512.0, player2Rating)
            assertEquals("Player2's Team", player2Team)
            assertEquals(0.5, komi)
            assertEquals("2025-01-19", date)
            assertEquals("A game for SGF parser testing", description)
            assertEquals("Comment to node", comment)
            assertEquals("Amsterdam, Netherlands", place)
            assertEquals("Test event", event)
            assertEquals("Empty", opening)
            assertEquals("Ivan Kochurkin", annotator)
            assertEquals("@", copyright)
            assertEquals("https://zagram.org/eidokropki/index.html", source)
            assertEquals(300.0, time)
            assertEquals("0+25", overtime)
            assertEquals(AppInfo("https://zagram.org/eidokropki/index.html", "1"), appInfo)
        }
    }

    @Test
    fun multipleGames() {
        val games = parseAndConvert("""
            (;GM[40]FF[4]SZ[39:32]GN[game 1])
            (;GM[40]FF[4]SZ[20:20]GN[game 2])
        """.trimIndent()
        )
        val (gameInfo0, rules0) = games[0]
        assertEquals(39, rules0.width)
        assertEquals(32, rules0.height)
        assertEquals("game 1", gameInfo0.gameName)
        val (gameInfo1, rules1) = games[1]
        assertEquals(20, rules1.width)
        assertEquals(20, rules1.height)
        assertEquals("game 2", gameInfo1.gameName)
    }

    @Test
    fun emptyFile() {
        assertTrue(
            parseAndConvert(
                "", listOf(
                    SgfDiagnostic(
                        "Empty game trees.",
                        LineColumn(1, 1),
                        SgfDiagnosticSeverity.Warning
                    )
                )
            ).isEmpty()
        )
    }

    @Test
    fun noNodes() {
        assertTrue(
            parseAndConvert(
                "()", listOf(
                    SgfDiagnostic(
                        "Root node with game info is missing.",
                        LineColumn(1, 2),
                        SgfDiagnosticSeverity.Error
                    )
                )
            ).isEmpty()
        )
    }

    @Test
    fun requiredPropertiesWithInvalidValues() {
        assertTrue(
            parseAndConvert(
                "(;GM[1]FF[3]SZ[1234:5678])",
                listOf(
                    SgfDiagnostic(
                        "Property GM (Game Mode) has unsupported value `1` (Go). The only `40` (Kropki) is supported.",
                        LineColumn(1, 6),
                        SgfDiagnosticSeverity.Critical
                    ),
                    SgfDiagnostic(
                        "Property FF (File Format) has unsupported value `3`. The only `4` is supported.",
                        LineColumn(1, 11),
                        SgfDiagnosticSeverity.Critical
                    ),
                    SgfDiagnostic(
                        "Property SZ (Size) has invalid width: `1234`. Expected: 0..254.",
                        LineColumn(1, 16),
                        SgfDiagnosticSeverity.Critical
                    ),
                    SgfDiagnostic(
                        "Property SZ (Size) has invalid height: `5678`. Expected: 0..254.",
                        LineColumn(1, 21),
                        SgfDiagnosticSeverity.Critical
                    ),
                )
            ).isEmpty()
        )
    }

    @Test
    fun differentGameModeAndSizeValues() {
        parseAndConvert(
            "(;GM[100]FF[4]SZ[20])", listOf(
                SgfDiagnostic(
                    "Property GM (Game Mode) has unsupported value `100`. The only `40` (Kropki) is supported.",
                    LineColumn(1, 6),
                    SgfDiagnosticSeverity.Critical
                )
            )
        )
        parseAndConvert("(;GM[40]FF[4]SZ[20])")
        parseAndConvert(
            "(;GM[40]FF[4]SZ[str])", listOf(
                SgfDiagnostic(
                    "Property SZ (Size) has invalid value `str`. Expected: 0..254.",
                    LineColumn(1, 17),
                    SgfDiagnosticSeverity.Critical,
                )
            )
        )
        parseAndConvert(
            "(;GM[40]FF[4]SZ[30:31:32])",
            listOf(
                SgfDiagnostic(
                    "Property SZ (Size) is defined in incorrect format: `30:31:32`. Expected: INT or INT:INT.",
                    LineColumn(1, 17),
                    SgfDiagnosticSeverity.Critical
                )
            )
        )
    }

    @Test
    fun requiredPropertiesMissing() {
        assertTrue(
            parseAndConvert(
                "(;)", listOf(
                    SgfDiagnostic(
                        "Property GM (Game Mode) should be specified.",
                        LineColumn(1, 3),
                        SgfDiagnosticSeverity.Error
                    ),
                    SgfDiagnostic(
                        "Property FF (File Format) should be specified.",
                        LineColumn(1, 3),
                        SgfDiagnosticSeverity.Error
                    ),
                    SgfDiagnostic(
                        "Property SZ (Size) should be specified.",
                        LineColumn(1, 3),
                        SgfDiagnosticSeverity.Critical
                    ),
                )
            ).isEmpty()
        )
    }

    @Test
    fun duplicatedProperties() {
        parseAndConvert(
            "(;GM[40]GM[40]FF[4]PB[Player1]SZ[39:32]PB[Player11]PB)", listOf(
                SgfDiagnostic(
                    "Property GM (Game Mode) is duplicated and ignored.",
                    LineColumn(1, 9),
                    SgfDiagnosticSeverity.Warning
                ),
                SgfDiagnostic(
                    "Property PB (Player1 Name) is duplicated and ignored.",
                    LineColumn(1, 40),
                    SgfDiagnosticSeverity.Warning
                ),
                SgfDiagnostic(
                    "Property PB (Player1 Name) is unspecified.",
                    LineColumn(1, 54),
                    SgfDiagnosticSeverity.Error
                ),
                SgfDiagnostic(
                    "Property PB (Player1 Name) is duplicated and ignored.",
                    LineColumn(1, 52),
                    SgfDiagnosticSeverity.Warning
                ),
            )
        )
    }

    @Test
    fun propertyHasMultipleValues() {
        parseAndConvert(
            "(;GM[40][1]FF[4]PB[Player1][Player11]SZ[39:32])",
            listOf(
                SgfDiagnostic("Property GM (Game Mode) has unsupported value `1` (Go). The only `40` (Kropki) is supported.", LineColumn(1, 10), SgfDiagnosticSeverity.Critical),
                SgfDiagnostic("Property GM (Game Mode) has duplicated value `1` that's ignored.", LineColumn(1, 10), SgfDiagnosticSeverity.Warning),
                SgfDiagnostic("Property PB (Player1 Name) has duplicated value `Player11` that's ignored.", LineColumn(1, 29), SgfDiagnosticSeverity.Warning),
            )
        )
    }

    @Test
    fun unknownProperties() {
        parseAndConvert(
            "(;GM[40]FF[4]SZ[39:32]UP[text value]UP)", listOf(
                SgfDiagnostic("Property UP is unknown.", LineColumn(1, 23), SgfDiagnosticSeverity.Warning),
                SgfDiagnostic("Property UP is unknown.", LineColumn(1, 37), SgfDiagnosticSeverity.Warning),
            )
        )
    }

    @Test
    fun timeLeft() {
        val gameTree = parseAndConvert(
            "(;GM[40]FF[4]SZ[39:32]BL[315]WL[312];B[bc]BL[308.3];W[de]WL[294.23])",
        ).single().gameTree
        assertEquals(gameTree.player1TimeLeft, 315.0)
        assertEquals(gameTree.player2TimeLeft, 312.0)
        var nextNode = gameTree.rootNode.getNextNode(2, 3, Player.First)!!
        assertEquals(308.3, nextNode.timeLeft)
        nextNode = nextNode.getNextNode(4, 5, Player.Second)!!
        assertEquals(294.23, nextNode.timeLeft)
    }

    @Test
    fun incorrectFormatWarnings() {
        val gameInfo = parseAndConvert(
            "(;GM[40]FF[4]SZ[39:32]BR[asdf])", listOf(
                SgfDiagnostic(
                    "Property BR (Player1 Rating) has incorrect format: `asdf`. Expected: Real Number.",
                    LineColumn(1, 26),
                    SgfDiagnosticSeverity.Warning
                ),
            )
        ).single().gameInfo
        assertNull(gameInfo.player1Rating)
    }
}

internal fun parseAndConvert(input: String, expectedDiagnostics: List<SgfDiagnostic> = emptyList()): List<Game> {
    val sgf = SgfParser.parse(input)
    val actualDiagnostics = mutableListOf<SgfDiagnostic>()
    val result = SgfConverter.convert(sgf) {
        actualDiagnostics.add(it)
    }
    assertEquals(expectedDiagnostics, actualDiagnostics)
    return result
}

internal fun checkMoveDisregardExtraInfo(expectedPosition: Position, expectedPlayer: Player, actualMoveInfo: MoveInfo) {
    assertEquals(expectedPosition, actualMoveInfo.position)
    assertEquals(expectedPlayer, actualMoveInfo.player)
}

internal fun GameTreeNode.getNextNode(x: Int, y: Int, player: Player): GameTreeNode? {
    return nextNodes[PositionPlayer(Position(x, y), player)]
}