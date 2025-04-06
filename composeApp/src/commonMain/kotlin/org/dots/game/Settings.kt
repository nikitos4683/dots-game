package org.dots.game

import DumpParameters
import org.dots.game.core.BaseMode
import org.dots.game.core.MoveInfo
import org.dots.game.core.Player
import org.dots.game.core.Position
import org.dots.game.core.Rules
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

private const val firstLevelSeparator = ","
private const val secondLevelSeparator = ";"

fun loadRules(): Rules {
    with (appSettings) {
        return Rules(
            getInt(Rules::width.rulesSettingName, Rules.Standard.width),
            getInt(Rules::height.rulesSettingName, Rules.Standard.height),
            captureByBorder = getBoolean(Rules::captureByBorder.rulesSettingName, Rules.Standard.captureByBorder),
            baseMode = BaseMode.valueOf(getString(Rules::baseMode.rulesSettingName, Rules.Standard.baseMode.name)),
            suicideAllowed = getBoolean(Rules::suicideAllowed.rulesSettingName, Rules.Standard.suicideAllowed),
            initialMoves = getStringOrNull(Rules::initialMoves.rulesSettingName)?.let { initialMovesData ->
                try {
                    buildList {
                        initialMovesData.split(secondLevelSeparator).forEach { moveInfo ->
                            val parts = moveInfo.split(firstLevelSeparator)
                            add(MoveInfo(Position(parts[0].toInt(), parts[1].toInt()), Player.valueOf(parts[2])))
                        }
                    }
                }
                catch (e: Exception) {
                    println("Error while reading ${Rules::initialMoves.rulesSettingName} `${initialMovesData}` (${e.message})")
                    null
                }
            } ?: listOf()
        )
    }
}

fun saveRules(rules: Rules) {
    with (appSettings) {
        putInt(Rules::width.rulesSettingName, rules.width)
        putInt(Rules::height.rulesSettingName, rules.height)
        putBoolean(Rules::captureByBorder.rulesSettingName, rules.captureByBorder)
        putString(Rules::baseMode.rulesSettingName, rules.baseMode.name)
        putBoolean(Rules::suicideAllowed.rulesSettingName, rules.suicideAllowed)
        putString(
            Rules::initialMoves.rulesSettingName,
            rules.initialMoves.joinToString(secondLevelSeparator) { "${it.position.x}$firstLevelSeparator${it.position.y}$firstLevelSeparator${it.player.name}" })
    }
}

private val KProperty<*>.rulesSettingName: String
    get() = Rules::class.settingName(this)

fun loadDumpParameters(): DumpParameters {
    with (appSettings) {
        return DumpParameters(
            printNumbers = getBoolean(DumpParameters::printNumbers.dumpSettingName, DumpParameters.DEFAULT.printNumbers),
            padding = getInt(DumpParameters::padding.dumpSettingName, DumpParameters.DEFAULT.padding),
            printCoordinates = getBoolean(DumpParameters::printCoordinates.dumpSettingName, DumpParameters.DEFAULT.printCoordinates),
            debugInfo = getBoolean(DumpParameters::debugInfo.dumpSettingName, DumpParameters.DEFAULT.debugInfo)
        )
    }
}

fun saveDumpParameters(dumpParameters: DumpParameters) {
    with (appSettings) {
        putBoolean(DumpParameters::printNumbers.dumpSettingName, dumpParameters.printNumbers)
        putInt(DumpParameters::padding.dumpSettingName, dumpParameters.padding)
        putBoolean(DumpParameters::printCoordinates.dumpSettingName, dumpParameters.printCoordinates)
        putBoolean(DumpParameters::debugInfo.dumpSettingName, dumpParameters.debugInfo)
    }
}

private val KProperty<*>.dumpSettingName: String
    get() = DumpParameters::class.settingName(this)

private fun KClass<*>.settingName(property: KProperty<*>): String = "${simpleName!!}.${property.name}"