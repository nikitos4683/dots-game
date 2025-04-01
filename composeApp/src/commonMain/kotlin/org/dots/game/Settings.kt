package org.dots.game

import org.dots.game.core.BaseMode
import org.dots.game.core.MoveInfo
import org.dots.game.core.Player
import org.dots.game.core.Position
import org.dots.game.core.Rules

private const val firstLevelSeparator = ","
private const val secondLevelSeparator = ";"

fun readRules(): Rules {
    with (appSettings) {
        return Rules(
            getInt(Rules::width.name, Rules.Standard.width),
            getInt(Rules::height.name, Rules.Standard.height),
            captureByBorder = getBoolean(Rules::captureByBorder.name, Rules.Standard.captureByBorder),
            baseMode = BaseMode.valueOf(getString(Rules::baseMode.name, Rules.Standard.baseMode.name)),
            suicideAllowed = getBoolean(Rules::suicideAllowed.name, Rules.Standard.suicideAllowed),
            initialMoves = getStringOrNull(Rules::initialMoves.name)?.let { initialMovesData ->
                try {
                    buildList {
                        initialMovesData.split(secondLevelSeparator).forEach { moveInfo ->
                            val parts = moveInfo.split(firstLevelSeparator)
                            add(MoveInfo(Position(parts[0].toInt(), parts[1].toInt()), Player.valueOf(parts[2])))
                        }
                    }
                }
                catch (e: Exception) {
                    println("Error while reading ${Rules::initialMoves.name} `${initialMovesData}` (${e.message})")
                    null
                }
            } ?: listOf()
        )
    }
}

fun writeRules(rules: Rules) {
    with (appSettings) {
        putInt(Rules::width.name, rules.width)
        putInt(Rules::height.name, rules.height)
        putBoolean(Rules::captureByBorder.name, rules.captureByBorder)
        putString(Rules::baseMode.name, rules.baseMode.name)
        putBoolean(Rules::suicideAllowed.name, rules.suicideAllowed)
        putString(
            Rules::initialMoves.name,
            rules.initialMoves.joinToString(secondLevelSeparator) { "${it.position.x}$firstLevelSeparator${it.position.y}$firstLevelSeparator${it.player.name}" })
    }
}