package org.dots.game.sgf

import org.dots.game.core.AppInfo
import org.dots.game.core.AppType
import org.dots.game.core.Game
import org.dots.game.core.GameResult
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
        val (gameInfo, rules) = parseConvertAndCheck(
            "(;GM[40]FF[4]CA[UTF-8]SZ[17:21]RU[russian]GN[Test Game]PB[Player1]BR[256]BT[Player1's Team]PW[Player2]WR[512]WT[Player2's Team]KM[0.5]DT[2025-01-19]GC[A game for SGF parser testing]C[Comment to node]PC[Amsterdam, Netherlands]EV[Test event]ON[Empty]AN[Ivan Kochurkin]CP[@]SO[https://zagram.org/eidokropki/index.html]TM[300]OT[0+25]AP[https\\://zagram.org/eidokropki/index.html:1]RO[1 (final)])",
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
            assertEquals("1 (final)", round)
        }
    }

    private val multiGamesSgf = """
            (;GM[40]FF[4]SZ[39:32]GN[game 1])
            (;GM[40]FF[4]SZ[20:20]GN[game 2])
        """.trimIndent()

    @Test
    fun multipleGames() {
        val games = parseConvertAndCheck(multiGamesSgf)
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
    fun multipleGamesWarning() {
        parseConvertAndCheck(
            multiGamesSgf,
            listOf(
                SgfLineColumnDiagnostic(
                    "Only single game is supported. Other games will be ignored.", LineColumn(2, 1),
                    SgfDiagnosticSeverity.Warning
                )
            ),
            warnOnMultipleGames = true
        )
    }

    @Test
    fun emptyFile() {
        assertTrue(
            parseConvertAndCheck(
                "", listOf(
                    SgfLineColumnDiagnostic(
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
            parseConvertAndCheck(
                "()", listOf(
                    SgfLineColumnDiagnostic(
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
            parseConvertAndCheck(
                "(;GM[1]FF[3]SZ[1234:5678])",
                listOf(
                    SgfLineColumnDiagnostic(
                        "Property GM (Game Mode) has unsupported value `1` (Go). The only `40` (Kropki) is supported.",
                        LineColumn(1, 6),
                        SgfDiagnosticSeverity.Critical
                    ),
                    SgfLineColumnDiagnostic(
                        "Property FF (File Format) has unsupported value `3`. The only `4` is supported.",
                        LineColumn(1, 11),
                        SgfDiagnosticSeverity.Critical
                    ),
                    SgfLineColumnDiagnostic(
                        "Property SZ (Size) has invalid width: `1234`. Expected: 0..254.",
                        LineColumn(1, 16),
                        SgfDiagnosticSeverity.Critical
                    ),
                    SgfLineColumnDiagnostic(
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
        parseConvertAndCheck(
            "(;GM[100]FF[4]SZ[20])", listOf(
                SgfLineColumnDiagnostic(
                    "Property GM (Game Mode) has unsupported value `100`. The only `40` (Kropki) is supported.",
                    LineColumn(1, 6),
                    SgfDiagnosticSeverity.Critical
                )
            )
        )
        parseConvertAndCheck("(;GM[40]FF[4]SZ[20])")
        parseConvertAndCheck(
            "(;GM[40]FF[4]SZ[str])", listOf(
                SgfLineColumnDiagnostic(
                    "Property SZ (Size) has invalid value `str`. Expected: 0..254.",
                    LineColumn(1, 17),
                    SgfDiagnosticSeverity.Critical,
                )
            )
        )
        parseConvertAndCheck(
            "(;GM[40]FF[4]SZ[30:31:32])",
            listOf(
                SgfLineColumnDiagnostic(
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
            parseConvertAndCheck(
                "(;)", listOf(
                    SgfLineColumnDiagnostic(
                        "Property GM (Game Mode) should be specified.",
                        LineColumn(1, 3),
                        SgfDiagnosticSeverity.Error
                    ),
                    SgfLineColumnDiagnostic(
                        "Property FF (File Format) should be specified.",
                        LineColumn(1, 3),
                        SgfDiagnosticSeverity.Error
                    ),
                    SgfLineColumnDiagnostic(
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
        parseConvertAndCheck(
            "(;GM[40]GM[40]FF[4]PB[Player1]SZ[39:32]PB[Player11]PB)", listOf(
                SgfLineColumnDiagnostic(
                    "Property GM (Game Mode) is duplicated and ignored.",
                    LineColumn(1, 9),
                    SgfDiagnosticSeverity.Warning
                ),
                SgfLineColumnDiagnostic(
                    "Property PB (Player1 Name) is duplicated and ignored.",
                    LineColumn(1, 40),
                    SgfDiagnosticSeverity.Warning
                ),
                SgfLineColumnDiagnostic(
                    "Property PB (Player1 Name) is unspecified.",
                    LineColumn(1, 54),
                    SgfDiagnosticSeverity.Error
                ),
                SgfLineColumnDiagnostic(
                    "Property PB (Player1 Name) is duplicated and ignored.",
                    LineColumn(1, 52),
                    SgfDiagnosticSeverity.Warning
                ),
            )
        )
    }

    @Test
    fun propertyHasMultipleValues() {
        parseConvertAndCheck(
            "(;GM[40][1]FF[4]PB[Player1][Player11]SZ[39:32])",
            listOf(
                SgfLineColumnDiagnostic("Property GM (Game Mode) has unsupported value `1` (Go). The only `40` (Kropki) is supported.", LineColumn(1, 10), SgfDiagnosticSeverity.Critical),
                SgfLineColumnDiagnostic("Property GM (Game Mode) has duplicated value `1` that's ignored.", LineColumn(1, 10), SgfDiagnosticSeverity.Warning),
                SgfLineColumnDiagnostic("Property PB (Player1 Name) has duplicated value `Player11` that's ignored.", LineColumn(1, 29), SgfDiagnosticSeverity.Warning),
            )
        )
    }

    @Test
    fun unknownProperties() {
        parseConvertAndCheck(
            "(;GM[40]FF[4]SZ[39:32]UP[text value]UP)", listOf(
                SgfLineColumnDiagnostic("Property UP is unknown.", LineColumn(1, 23), SgfDiagnosticSeverity.Warning),
                SgfLineColumnDiagnostic("Property UP is unknown.", LineColumn(1, 37), SgfDiagnosticSeverity.Warning),
            )
        )
    }

    @Test
    fun timeLeft() {
        val gameTree = parseConvertAndCheck(
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
        val gameInfo = parseConvertAndCheck(
            "(;GM[40]FF[4]SZ[39:32]BR[asdf])", listOf(
                SgfLineColumnDiagnostic(
                    "Property BR (Player1 Rating) has incorrect format: `asdf`. Expected: Real Number.",
                    LineColumn(1, 26),
                    SgfDiagnosticSeverity.Warning
                ),
            )
        ).single().gameInfo
        assertNull(gameInfo.player1Rating)
    }

    @Test
    fun playdotsSgf() {
        val gameInfo = parseConvertAndCheck(
            "(;AP[Спортивные Точки (playdots.ru)]GM[40]FF[4]SZ[39:32]BR[Нет звания, 1200]WR[Второй разряд, 1300])"
        ).single().gameInfo

        assertEquals(AppType.Playdots, gameInfo.appInfo!!.appType)
        assertEquals(1200.0, gameInfo.player1Rating)
        assertEquals(1300.0, gameInfo.player2Rating)
    }

    @Test
    fun gameResultInvalid() {
        val valuesToErrors = listOf<Triple<String, SgfLineColumnDiagnostic, GameResult?>>(
            Triple("", SgfLineColumnDiagnostic("Property RE (Result) has invalid player ``. Allowed values: B or W", LineColumn(1, 26), SgfDiagnosticSeverity.Error), null),
            Triple("_", SgfLineColumnDiagnostic("Property RE (Result) has invalid player `_`. Allowed values: B or W", LineColumn(1, 26), SgfDiagnosticSeverity.Error), null),
            Triple("B_", SgfLineColumnDiagnostic("Property RE (Result) value `B_` is written in invalid format. Correct format is 0 (Draw) or X+Y where X is B or W, Y is Number, R (Resign), T (Time) or ? (Unknown)", LineColumn(1, 27), SgfDiagnosticSeverity.Error), GameResult.UnknownWin(Player.First)),
            Triple("B+", SgfLineColumnDiagnostic("Property RE (Result) has invalid result value ``. Correct value is Number, R (Resign), T (Time) or ? (Unknown)", LineColumn(1, 28), SgfDiagnosticSeverity.Error), GameResult.UnknownWin(Player.First)),
            Triple("B+X", SgfLineColumnDiagnostic("Property RE (Result) has invalid result value `X`. Correct value is Number, R (Resign), T (Time) or ? (Unknown)", LineColumn(1, 28), SgfDiagnosticSeverity.Error), GameResult.UnknownWin(Player.First)),
            Triple("B+R_", SgfLineColumnDiagnostic("Property RE (Result) has unexpected suffix `_`.", LineColumn(1, 29), SgfDiagnosticSeverity.Error), GameResult.ResignWin(Player.First)),
        )

        for ((value, diagnostic, expectedGameResult) in valuesToErrors) {
            val actualGameResult = parseConvertAndCheck(
                "(;GM[40]FF[4]SZ[39:32]RE[$value])",
                listOf(diagnostic)
            ).single().gameInfo.result
            if (expectedGameResult != null) {
                assertEquals(expectedGameResult, actualGameResult)
            }
        }
    }

    @Test
    fun gameResultValid() {
        val valuesToGameResults = listOf(
            Triple("0", GameResult.Draw, null),
            Triple("B+R", GameResult.ResignWin(Player.First), null),
            Triple("B+T" , GameResult.TimeWin(Player.First), null),
            Triple("B+?" , GameResult.UnknownWin(Player.First), null),
            Triple("B+10" , GameResult.ScoreWin(10.0, Player.First),
                SgfLineColumnDiagnostic("Property RE (Result) has value `10` that doesn't match score from field `0`.", LineColumn(1, 23), SgfDiagnosticSeverity.Warning)
            ),
            Triple("W+5" , GameResult.ScoreWin(5.0, Player.Second),
                SgfLineColumnDiagnostic("Property RE (Result) has value `5` that doesn't match score from field `0`.", LineColumn(1, 23), SgfDiagnosticSeverity.Warning)
            ),
        )

        for ((value, expectedGameResult, diagnostic) in valuesToGameResults) {
            val actualGameResult = parseConvertAndCheck(
                "(;GM[40]FF[4]SZ[39:32]RE[$value])",
                listOfNotNull(diagnostic)
            ).single().gameInfo.result
            assertEquals(expectedGameResult, actualGameResult)
        }
    }
}

internal fun parseConvertAndCheck(input: String, expectedDiagnostics: List<SgfLineColumnDiagnostic> = emptyList(), warnOnMultipleGames: Boolean = false): List<Game> {
    val lineOffsets by lazy(LazyThreadSafetyMode.NONE) { input.buildLineOffsets() }
    val actualDiagnostics = mutableListOf<SgfLineColumnDiagnostic>()
    val games = Sgf.parseAndConvert(input, warnOnMultipleGames) {
        actualDiagnostics.add(it.toLineColumnDiagnostic(lineOffsets))
    }
    assertEquals(expectedDiagnostics, actualDiagnostics)
    return games
}

internal fun checkMoveDisregardExtraInfo(expectedPosition: Position, expectedPlayer: Player, actualMoveInfo: MoveInfo) {
    assertEquals(expectedPosition, actualMoveInfo.position)
    assertEquals(expectedPlayer, actualMoveInfo.player)
}

internal fun GameTreeNode.getNextNode(x: Int, y: Int, player: Player): GameTreeNode? {
    return nextNodes[PositionPlayer(Position(x, y), player)]
}