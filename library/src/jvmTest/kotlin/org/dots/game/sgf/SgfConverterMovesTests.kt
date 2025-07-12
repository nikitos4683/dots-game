package org.dots.game.sgf

import org.dots.game.DiagnosticSeverity
import org.dots.game.LineColumn
import org.dots.game.LineColumnDiagnostic
import org.dots.game.core.Label
import org.dots.game.core.Player
import org.dots.game.core.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SgfConverterMovesTests {
    @Test
    fun initialPositionsAreCorrect() {
        val rules = parseConvertAndCheck(
            "(;GM[40]FF[4]SZ[62:62]AB[az][mm]AW[AZ][])"
        ).single().rules
        assertEquals(3, rules.initialMoves.size)
        checkMoveDisregardExtraInfo(Position(1, 26), Player.First, rules.initialMoves[0])
        checkMoveDisregardExtraInfo(Position(13, 13), Player.First,rules.initialMoves[1])
        checkMoveDisregardExtraInfo(Position(27, 52), Player.Second,rules.initialMoves[2])
    }

    @Test
    fun initialPositionsAreIncorrect() {
        val rules = parseConvertAndCheck(
            "(;GM[40]FF[4]SZ[39:32]AB[a!]AW[-Z][1234])", listOf(
                LineColumnDiagnostic(
                    "Property AB (Player1 initial dots) has incorrect y coordinate `!`.",
                    LineColumn(1, 27),
                    DiagnosticSeverity.Error
                ),
                LineColumnDiagnostic(
                    "Property AW (Player2 initial dots) has incorrect x coordinate `-`.",
                    LineColumn(1, 32),
                    DiagnosticSeverity.Error
                ),
                LineColumnDiagnostic(
                    "Property AW (Player2 initial dots) has incorrect x coordinate `1`.",
                    LineColumn(1, 36),
                    DiagnosticSeverity.Error
                ),
                LineColumnDiagnostic(
                    "Property AW (Player2 initial dots) has incorrect y coordinate `2`.",
                    LineColumn(1, 37),
                    DiagnosticSeverity.Error
                ),
                LineColumnDiagnostic(
                    "Property AW (Player2 initial dots) has incorrect extra chars: `34`",
                    LineColumn(1, 38),
                    DiagnosticSeverity.Error
                ),
            )
        ).single().rules
        assertTrue(rules.initialMoves.isEmpty())
    }

    @Test
    fun initialPositionsOverwriting() {
        val rules = parseConvertAndCheck(
            "(;GM[40]FF[4]SZ[39:32]AB[ab][mm][ab])", listOf(
                LineColumnDiagnostic(
                    "Property AB (Player1 initial dots) value `ab` overwrites one the previous position.",
                    LineColumn(1, 34),
                    DiagnosticSeverity.Warning
                ),
            )
        ).single().rules
        assertEquals(2, rules.initialMoves.size)
        checkMoveDisregardExtraInfo(Position(13, 13), Player.First,rules.initialMoves[0])
        checkMoveDisregardExtraInfo(Position(1, 2), Player.First,rules.initialMoves[1])
    }

    @Test
    fun initialPositionsOfPlayer2OverwritesPlayer1() {
        val rules = parseConvertAndCheck(
            "(;GM[40]FF[4]SZ[39:32]AB[ab]AW[ab])", listOf(
                LineColumnDiagnostic(
                    "Property AW (Player2 initial dots) value `ab` overwrites one the previous position of first player AB (Player1 initial dots).",
                    LineColumn(1, 32),
                    DiagnosticSeverity.Warning
                ),
            )
        ).single().rules
        checkMoveDisregardExtraInfo(Position(1, 2), Player.Second, rules.initialMoves.single())
    }

    @Test
    fun initialPositionsIncorrectBecauseOfPlacedToCapturedTerritory() {
        // . .  *1 *2 .
        // . *0 +6 +7 *3
        // . .  *5 *4 .
        parseConvertAndCheck(
            "(;GM[40]FF[4]SZ[39:32]AB[bb][ca][da][eb][dc][cc]AW[cb][db])", listOf(
                LineColumnDiagnostic(
                    "Property AW (Player2 initial dots) has incorrect value `db`. The dot at position (4;2) is already placed or captured (move number: 8).",
                    LineColumn(1, 56),
                    DiagnosticSeverity.Error,
                )
            )
        )
    }

    @Test
    fun branches() {
        val rootNode = parseConvertAndCheck(
            "(;GM[40]FF[4]SZ[39:32];B[bb](;B[bc];W[bd])(;B[cc];W[cd]))"
        ).single().gameTree.rootNode
        assertEquals(1, rootNode.nextNodes.size)
        val mainBranchNode = rootNode.getNextNode(2, 2, Player.First)!!
        assertEquals(2, mainBranchNode.nextNodes.size)

        var branch1Node = mainBranchNode.getNextNode(2, 3, Player.First)!!
        branch1Node = branch1Node.getNextNode(2, 4, Player.Second)!!
        assertTrue(branch1Node.nextNodes.isEmpty())

        var branch2Node = mainBranchNode.getNextNode(3, 3, Player.First)!!
        branch2Node = branch2Node.getNextNode(3, 4, Player.Second)!!
        assertTrue(branch2Node.nextNodes.isEmpty())
    }

    @Test
    fun incorrectMovesSequence() {
        val rootNode = parseConvertAndCheck(
            "(;GM[40]FF[4]SZ[10:10];B[bb];B[__];W[bb];W[c];W[ml])", listOf(
                LineColumnDiagnostic(
                    "Property B (Player1 move) has incorrect x coordinate `_`.",
                    LineColumn(1, 32),
                    DiagnosticSeverity.Error
                ),
                LineColumnDiagnostic(
                    "Property B (Player1 move) has incorrect y coordinate `_`.",
                    LineColumn(1, 33),
                    DiagnosticSeverity.Error
                ),
                LineColumnDiagnostic(
                    "Property W (Player2 move) has incorrect value `bb`. The dot at position (2;2) is already placed or captured (move number: 2).",
                    LineColumn(1, 38),
                    DiagnosticSeverity.Error
                ),
                LineColumnDiagnostic(
                    "Property W (Player2 move) has incorrect y coordinate ``.",
                    LineColumn(1, 45),
                    DiagnosticSeverity.Error
                ),
                LineColumnDiagnostic(
                    "Property W (Player2 move) has incorrect value `ml`. The position (13;12) is out of bounds 10:10 (move number: 2).",
                    LineColumn(1, 49),
                    DiagnosticSeverity.Error
                ),
            )
        ).single().gameTree.rootNode
        val nextNode = rootNode.getNextNode(2, 2, Player.First)!!
        assertEquals(1, nextNode.nextNodes.size)
    }

    @Test
    fun movesInRootNode() {
        val rootNode = parseConvertAndCheck(
            "(;GM[40]FF[4]SZ[39:32]B[cc]W[dd])",
            listOf(
                LineColumnDiagnostic(
                    "Property B (Player1 move) declared in Root scope, but should be declared in Move scope.",
                    LineColumn(1, 23),
                    DiagnosticSeverity.Warning
                ),
                LineColumnDiagnostic(
                    "Property W (Player2 move) declared in Root scope, but should be declared in Move scope.",
                    LineColumn(1, 28),
                    DiagnosticSeverity.Warning
                ),
            )
        ).single().gameTree.rootNode
        var nextNode = rootNode.getNextNode(3, 3, Player.First)!!
        nextNode = nextNode.getNextNode(4, 4, Player.Second)!!
        assertTrue(nextNode.nextNodes.isEmpty())
    }

    @Test
    fun gameInfoInMoveNode() {
        val gameTree = parseConvertAndCheck(
            "(;GM[40]FF[4]SZ[39:32];GN[Game name not in root]B[cc])",
            listOf(
                LineColumnDiagnostic(
                    "Property GN (Game Name) declared in Move scope, but should be declared in Root scope. The value is ignored.",
                    LineColumn(1, 24),
                    DiagnosticSeverity.Error
                ),
            )
        ).single().gameTree
        val nextNode = gameTree.rootNode.getNextNode(3, 3, Player.First)!!
        assertTrue(nextNode.nextNodes.isEmpty())
    }

    @Test
    fun capturingPositions() {
        // .  *2  .
        // *1 +0 *3
        // .  *4  .
        val gameTree = parseConvertAndCheck(
            "(;GM[40]FF[4]AP[zagram.org]SZ[39:32];B[bb];W[ab];W[ba];W[cb];W[bc.bccbbaabbc])", listOf(
                LineColumnDiagnostic(
                    "Property W (Player2 move) has capturing positions that are not yet supported: (2;3), (3;2), (2;1), (1;2), (2;3) (`bccbbaabbc`). The capturing is calculated automatically according game rules.",
                    LineColumn(1, 67),
                    DiagnosticSeverity.Warning
                ),
            )
        ).single().gameTree

        var node = gameTree.rootNode.getNextNode(2, 2, Player.First)!!
        node = node.getNextNode(1, 2, Player.Second)!!
        node = node.getNextNode(2, 1, Player.Second)!!
        node = node.getNextNode(3, 2, Player.Second)!!
        node = node.getNextNode(2, 3, Player.Second)!!

        assertEquals(
            listOf(
                Position(2, 3),
                Position(3, 2),
                Position(2, 1),
                Position(1, 2),
            ),
            node.moveResult!!.bases!!.single().closurePositions
        )
    }

    @Test
    fun grounding() {
        fun createInput(firstPlayerWinsByProperty: Boolean, firstPlayerWinsByField: Boolean): String {
            return "(;GM[40]FF[4]SZ[39:32]RE[${if (firstPlayerWinsByProperty) "B+G" else "Draw"}];W[${if (firstPlayerWinsByField) "bb" else "aa"}])"
        }

        parseConvertAndCheck(createInput(firstPlayerWinsByProperty = true, firstPlayerWinsByField = true))
        // Draw may occur with any score
        parseConvertAndCheck(createInput(firstPlayerWinsByProperty = false, firstPlayerWinsByField = true))
        parseConvertAndCheck(createInput(firstPlayerWinsByProperty = true, firstPlayerWinsByField = false), listOf(
            LineColumnDiagnostic(
                "Property RE (Result) has `First` player as winner but the result of the game from field: Draw.",
                LineColumn(1, 37),
                DiagnosticSeverity.Warning,
            )
        ))
        parseConvertAndCheck(createInput(firstPlayerWinsByProperty = false, firstPlayerWinsByField = false))
    }

    @Test
    fun definedWinGameResultByRePropertyDoesntMatchResultFromField() {
        parseConvertAndCheck(
            "(;GM[40]FF[4]SZ[39:32]RE[W+2];B[bb];W[ab];W[ba];W[cb];W[bc])", listOf(
                LineColumnDiagnostic(
                    "Property RE (Result) has value `2` that doesn't match score from game field `1`.",
                    LineColumn(1, 61),
                    DiagnosticSeverity.Warning
                )
            )
        )
    }

    @Test
    fun definedWinGameResultByRePropertyDoesntMatchResultFromFieldInSubBranch() {
        parseConvertAndCheck(
            "(;GM[40]FF[4]SZ[39:32]RE[W+2];B[bb];W[ab];W[ba];W[cb];W[bc](;B[dd]))", listOf(
                LineColumnDiagnostic(
                    "Property RE (Result) has value `2` that doesn't match score from game field `1`.",
                    LineColumn(1, 68),
                    DiagnosticSeverity.Warning
                )
            )
        )
    }

    @Test
    fun ignoreFieldResultScoreValidationInSecondaryBranches() {
        parseConvertAndCheck(
            "(;GM[40]FF[4]SZ[39:32]RE[W+1];B[bb];W[ab];W[ba];W[cb];W[bc](;B[dd])(;B[cc];W[dc];W[cd]))"
        )
    }

    @Test
    fun movesComment() {
        val firstComment = "Player 1 starts the game"
        val secondComment = "Player 2 surrounds a dot"

        val gameTree = parseConvertAndCheck(
            "(;GM[40]FF[4]SZ[39:32];B[bb]C[$firstComment];W[ab];W[ba];W[cb];W[bc]C[$secondComment])"
        ).single().gameTree

        gameTree.next()
        assertEquals(firstComment, gameTree.currentNode.comment)

        gameTree.rewindForward()
        assertEquals(secondComment, gameTree.currentNode.comment)
    }

    @Test
    fun labelsIncorrect() {
        parseConvertAndCheck("(;GM[40]FF[4]SZ[39:32];LB[aa])",
            listOf(
                LineColumnDiagnostic("Property LB (Label) has unexpected separator ``", LineColumn(1, 29))
            )).single().gameTree
        parseConvertAndCheck("(;GM[40]FF[4]SZ[39:32];LB[aa.])",
            listOf(
                LineColumnDiagnostic("Property LB (Label) has unexpected separator `.`", LineColumn(1, 29))
            )).single().gameTree
    }

    @Test
    fun labels() {
        val gameTree = parseConvertAndCheck("(;GM[40]FF[4]SZ[39:32];W[aa]LB[aa:])").single().gameTree
        gameTree.next()
        val label = gameTree.currentNode.labels!!.single()
        assertEquals(Position(1, 1), label.position)
        assertEquals("", label.text)

        val gameTree2 = parseConvertAndCheck("(;GM[40]FF[4]SZ[39:32];W[cc]LB[aa:label][bb:label2])").single().gameTree
        gameTree2.next()
        val labels = gameTree2.currentNode.labels
        assertEquals(listOf(Label(Position(1, 1), "label"), Label(Position(2, 2), "label2")), labels)
    }

    @Test
    fun circles() {
        val gameTree = parseConvertAndCheck("(;GM[40]FF[4]SZ[39:32];B[bb]CR[ba][cb][bc][ab])").single().gameTree
        gameTree.next()
        val circles = gameTree.currentNode.circles
        assertEquals(listOf(Position(2, 1), Position(3, 2), Position(2, 3), Position(1, 2)), circles)
    }

    @Test
    fun circlesIncorrect() {
        val gameTree = parseConvertAndCheck("(;GM[40]FF[4]SZ[39:32];B[bb]CR[ba.cd])", listOf(
            LineColumnDiagnostic("Property CR (Circle) has incorrect extra chars: `.cd`", LineColumn(1, 34))
        )).single().gameTree
        gameTree.next()
        val circles = gameTree.currentNode.circles
        assertEquals(listOf(Position(2, 1)), circles)
    }

    @Test
    fun squares() {
        val gameTree = parseConvertAndCheck("(;GM[40]FF[4]SZ[39:32];B[bb]SQ[ba][cb][bc][ab])").single().gameTree
        gameTree.next()
        val squares = gameTree.currentNode.squares
        assertEquals(listOf(Position(2, 1), Position(3, 2), Position(2, 3), Position(1, 2)), squares)
    }
}