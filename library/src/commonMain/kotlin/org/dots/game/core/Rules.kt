package org.dots.game.core

import org.dots.game.core.InitialPositionType.Cross
import org.dots.game.core.InitialPositionType.Empty

class Rules(
    val width: Int = 39,
    val height: Int = 32,
    val captureByBorder: Boolean = false,
    val baseMode: BaseMode = BaseMode.AtLeastOneOpponentDot,
    val suicideAllowed: Boolean = true,
    val initialMoves: List<MoveInfo> = Cross.generateDefaultInitialPositions(width, height) ?: emptyList(),
) {
    companion object {
        val Standard = Rules()
    }

    val initialPositionType: InitialPositionType by lazy {
        if (initialMoves.isEmpty()) {
            Empty
        } else {
            if (initialMoves.size == 4) {
                val sortedMoveInfos = initialMoves.sortedBy { it.position.squareDistanceToZero() }
                val firstMoveInfo =  sortedMoveInfos.first()
                val secondMoveInfo = sortedMoveInfos[1]
                val thirdMoveInfo = sortedMoveInfos[2]
                if (secondMoveInfo.position.squareDistanceTo(firstMoveInfo.position) == 1 &&
                    thirdMoveInfo.position.squareDistanceTo(firstMoveInfo.position) == 1 &&
                    secondMoveInfo.position != thirdMoveInfo.position &&
                    secondMoveInfo.player == thirdMoveInfo.player &&
                    secondMoveInfo.player != firstMoveInfo.player
                ) {
                    val fourthMoveInfo =  sortedMoveInfos[3]
                    if (fourthMoveInfo.position.squareDistanceTo(firstMoveInfo.position) == 2 &&
                            fourthMoveInfo.player == firstMoveInfo.player) {
                        return@lazy Cross
                    }
                }
            }
            InitialPositionType.Custom
        }
    }
}

enum class InitialPositionType {
    Empty,
    Cross,
    Custom;
}

enum class BaseMode {
    /**
     * The surrounding becomes captured if at least it captures at least one enemy's dot.
     * It doesn't imply inner holes since they could be encountered only in a case
     * when an inner empty base is surrounded by an outer inner base.
     * The case is very rare and generally in this case there is no sense to place dot inside such territories.
     * That's why inner base holes are not implied and surroundings are always created by a minimal territory
     * excluding inner holes.
     * Also, the dots game format is surrounding-based unlike go-format that is more filling-based
     * (it affects algorithms for searching surroundings).
     */
    AtLeastOneOpponentDot,

    /**
     * The surrounding becomes captured even without any dot inside.
     * Inner holes can't be created.
     */
    AnySurrounding,

    /**
     * Surrounding should not contain empty positions.
     * The mode makes the game more like Go.
     * Inner holes are possible because it removes game state contradictions and makes the game more attractive for
     * go players that used to play in this way.
     */
    AllOpponentDots;
}

fun InitialPositionType.generateDefaultInitialPositions(width: Int, height: Int): List<MoveInfo>? {
    when {
        this == Empty -> {
            return emptyList()
        }
        this == Cross -> {
            if (width < 2 || height < 2) return null

            val startPosition = Position(width / 2, height / 2)
            return listOf(
                MoveInfo(startPosition, Player.First),
                MoveInfo(Position(startPosition.x + 1, startPosition.y), Player.Second),
                MoveInfo(Position(startPosition.x + 1, startPosition.y + 1), Player.First),
                MoveInfo(Position(startPosition.x, startPosition.y + 1), Player.Second),
            )
        }
        else -> {
            return emptyList()
        }
    }
}