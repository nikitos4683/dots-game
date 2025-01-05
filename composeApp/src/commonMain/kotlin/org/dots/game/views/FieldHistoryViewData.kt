package org.dots.game.views

import org.dots.game.core.FieldHistory
import org.dots.game.core.FieldHistoryElements
import org.dots.game.core.Node
import org.dots.game.core.NodeHistoryElement
import org.dots.game.core.getHistoryElements
import kotlin.collections.set

class FieldHistoryViewData(fieldHistory: FieldHistory) {
    val fieldHistoryElements: FieldHistoryElements by lazy {
        fieldHistory.getHistoryElements(mainBranchIsAlwaysStraight = true)
    }

    val nodeToIndexMap: Map<Node, Pair<Int, Int>> by lazy {
        buildMap {
            for (xIndex in fieldHistoryElements.indices) {
                for ((yIndex, element) in fieldHistoryElements[xIndex].withIndex()) {
                    val node = (element as? NodeHistoryElement)?.node ?: continue
                    this[node] = xIndex to yIndex
                }
            }
        }
    }
}