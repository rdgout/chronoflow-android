package net.chronoflow.feature.trip.timeline.host

import androidx.compose.ui.unit.Density
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.plan.drawing.DayDrawingKey
import net.chronoflow.core.plan.trippage.TripPageLayout
import net.chronoflow.feature.trip.timeline.timelineTextScale
import kotlin.math.roundToInt

/** The width a day may use: the window minus both gutters and the side insets, in dp. */
fun timelineAvailableWidthDp(
    windowWidthDp: Double,
    safeLeftDp: Double,
    safeRightDp: Double,
): Double = windowWidthDp - 2 * TripPageLayout.resolve(windowWidthDp).gutter - safeLeftDp - safeRightDp

/** Shared by the page and the tests, so both compute the key a drawing is cached under. */
fun timelineDrawingKey(
    dayId: String,
    revision: String,
    availableWidthDp: Double,
    density: Density,
    dark: Boolean,
    compact: Boolean,
    language: Language,
): DayDrawingKey =
    DayDrawingKey(
        dayId = dayId,
        revision = revision,
        availableWidthPx = (availableWidthDp * density.density).roundToInt(),
        density = density.density,
        textScale = timelineTextScale(density),
        dark = dark,
        compact = compact,
        language = language,
    )
