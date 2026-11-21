package net.chronoflow.feature.settings.bodyclock

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import kotlin.math.roundToInt

@Stable
internal class BodyClockQuestionPositions {
    private val sections = mutableMapOf<BodyClockGroup, LayoutCoordinates>()
    private val questions = mutableMapOf<String, LayoutCoordinates>()

    fun sectionPlaced(group: BodyClockGroup, coordinates: LayoutCoordinates) {
        sections[group] = coordinates
    }

    fun questionPlaced(field: String, coordinates: LayoutCoordinates) {
        questions[field] = coordinates
    }

    fun offset(group: BodyClockGroup, field: String): Int? {
        val section = sections[group]?.takeIf { it.isAttached } ?: return null
        val question = questions[field]?.takeIf { it.isAttached } ?: return null
        return section.localPositionOf(question, Offset.Zero).y.roundToInt().coerceAtLeast(0)
    }
}
