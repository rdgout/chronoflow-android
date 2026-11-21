package net.chronoflow.feature.settings.bodyclock

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import kotlin.math.roundToInt

@Stable
internal class BodyClockQuestionPositions {
    private val sections = mutableMapOf<BodyClockGroup, LayoutCoordinates>()
    private val questions = mutableMapOf<String, LayoutCoordinates>()

    fun section(group: BodyClockGroup): Modifier = Modifier.onPlaced { sections[group] = it }

    fun question(field: String): Modifier = Modifier.onPlaced { questions[field] = it }

    fun offset(group: BodyClockGroup, field: String): Int? {
        val section = sections[group]?.takeIf { it.isAttached } ?: return null
        val question = questions[field]?.takeIf { it.isAttached } ?: return null
        return section.localPositionOf(question, Offset.Zero).y.roundToInt().coerceAtLeast(0)
    }
}
