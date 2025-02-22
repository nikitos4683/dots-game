package org.dots.game.sgf

import org.dots.game.core.Player
import org.dots.game.core.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SgfConverterMovesTests {
    @Test
    fun initialPositionsAreCorrect() {
        val rules = parseAndConvert(
            "(;GM[40]FF[4]SZ[100:100]AB[az][mm]AW[AZ][])"
        ).single().rules
        assertEquals(3, rules.initialMoves.size)
        checkMoveDisregardExtraInfo(Position(1, 26), Player.First, rules.initialMoves[0])
        checkMoveDisregardExtraInfo(Position(13, 13), Player.First,rules.initialMoves[1])
        checkMoveDisregardExtraInfo(Position(27, 52), Player.Second,rules.initialMoves[2])
    }

    @Test
    fun initialPositionsAreIncorrect() {
        val rules = parseAndConvert(
            "(;GM[40]FF[4]SZ[39:32]AB[a!]AW[-Z][1234])", listOf(
                SgfDiagnostic(
                    "Property AB (Player1 initial dots) has incorrect y coordinate `!`.",
                    LineColumn(1, 27),
                    SgfDiagnosticSeverity.Error
                ),
                SgfDiagnostic(
                    "Property AW (Player2 initial dots) has incorrect x coordinate `-`.",
                    LineColumn(1, 32),
                    SgfDiagnosticSeverity.Error
                ),
                SgfDiagnostic(
                    "Property AW (Player2 initial dots) has incorrect format: `1234`. Expected: `xy`, where each coordinate in [a..zA..Z].",
                    LineColumn(1, 36),
                    SgfDiagnosticSeverity.Error
                )
            )
        ).single().rules
        assertTrue(rules.initialMoves.isEmpty())
    }

    @Test
    fun initialPositionsOverwriting() {
        val rules = parseAndConvert(
            "(;GM[40]FF[4]SZ[39:32]AB[ab][mm][ab])", listOf(
                SgfDiagnostic(
                    "Property AB (Player1 initial dots) value `ab` overwrites one the previous position.",
                    LineColumn(1, 34),
                    SgfDiagnosticSeverity.Warning
                ),
            )
        ).single().rules
        assertEquals(2, rules.initialMoves.size)
        checkMoveDisregardExtraInfo(Position(13, 13), Player.First,rules.initialMoves[0])
        checkMoveDisregardExtraInfo(Position(1, 2), Player.First,rules.initialMoves[1])
    }

    @Test
    fun initialPositionsOfPlayer2OverwritesPlayer1() {
        val rules = parseAndConvert(
            "(;GM[40]FF[4]SZ[39:32]AB[ab]AW[ab])", listOf(
                SgfDiagnostic(
                    "Property AW (Player2 initial dots) value `ab` overwrites one the previous position of first player AB (Player1 initial dots).",
                    LineColumn(1, 32),
                    SgfDiagnosticSeverity.Warning
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
        parseAndConvert(
            "(;GM[40]FF[4]SZ[39:32]AB[bb][ca][da][eb][dc][cc]AW[cb][db])", listOf(
                SgfDiagnostic(
                    "Property AW (Player2 initial dots) value `db` is incorrect. The dot at position `(4;2)` is already placed or captured.",
                    LineColumn(1, 56),
                    SgfDiagnosticSeverity.Error,
                )
            )
        )
    }

    @Test
    fun branches() {
        val rootNode = parseAndConvert(
            "(;GM[40]FF[4]SZ[39:32];B[bb](;B[bc];W[bd])(;B[cc];W[cd]))"
        ).single().gameTree.rootNode
        assertEquals(3, rootNode.nextNodes.size)

        val branch0Node = rootNode.getNextNode(2, 2, Player.First)!!
        assertTrue(branch0Node.nextNodes.isEmpty())

        var branch1Node = rootNode.getNextNode(2, 3, Player.First)!!
        branch1Node = branch1Node.getNextNode(2, 4, Player.Second)!!
        assertTrue(branch1Node.nextNodes.isEmpty())

        var branch2Node = rootNode.getNextNode(3, 3, Player.First)!!
        branch2Node = branch2Node.getNextNode(3, 4, Player.Second)!!
        assertTrue(branch2Node.nextNodes.isEmpty())
    }

    @Test
    fun incorrectMovesSequence() {
        val rootNode = parseAndConvert(
            "(;GM[40]FF[4]SZ[39:32];B[bb];B[__];W[bb]", listOf(
                SgfDiagnostic("Property B (Player1 move) has incorrect x coordinate `_`.", LineColumn(1, 32), SgfDiagnosticSeverity.Error),
                SgfDiagnostic("Property B (Player1 move) has incorrect y coordinate `_`.", LineColumn(1, 33), SgfDiagnosticSeverity.Error),
                SgfDiagnostic("Property W (Player2 move) value `bb` is incorrect. The dot at position `(2;2)` is already placed or captured.", LineColumn(1, 38), SgfDiagnosticSeverity.Error),
            )
        ).single().gameTree.rootNode
        val nextNode = rootNode.getNextNode(2, 2, Player.First)!!
        assertTrue(nextNode.nextNodes.isEmpty())
    }

    @Test
    fun movesInRootNode() {
        val rootNode = parseAndConvert(
            "(;GM[40]FF[4]SZ[39:32]B[cc]W[dd])",
            listOf(
                SgfDiagnostic("Property B (Player1 move) declared in Root scope, but should be declared in Move scope.", LineColumn(1, 23), SgfDiagnosticSeverity.Warning),
                SgfDiagnostic("Property W (Player2 move) declared in Root scope, but should be declared in Move scope.", LineColumn(1, 28), SgfDiagnosticSeverity.Warning),
            )
        ).single().gameTree.rootNode
        var nextNode = rootNode.getNextNode(3, 3, Player.First)!!
        nextNode = nextNode.getNextNode(4, 4, Player.Second)!!
        assertTrue(nextNode.nextNodes.isEmpty())
    }

    @Test
    fun gameInfoInMoveNode() {
        val gameTree = parseAndConvert(
            "(;GM[40]FF[4]SZ[39:32];GN[Game name not in root]B[cc])",
            listOf(
                SgfDiagnostic("Property GN (Game Name) declared in Move scope, but should be declared in Root scope. The value is ignored.", LineColumn(1, 24), SgfDiagnosticSeverity.Error),
            )
        ).single().gameTree
        val nextNode = gameTree.rootNode.getNextNode(3, 3, Player.First)!!
        assertTrue(nextNode.nextNodes.isEmpty())
    }
}