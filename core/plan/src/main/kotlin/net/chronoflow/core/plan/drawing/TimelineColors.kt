package net.chronoflow.core.plan.drawing

import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.Priority
import net.chronoflow.core.plan.generated.PlanMetrics
import net.chronoflow.core.plan.generated.TimelinePalette
import kotlin.math.roundToInt

/** Colour rules of the drawing: group alpha is pre-composited over `surface2`, so the replay never needs a layer. */
object TimelineColors {
    /** The gradient gap colour; a constant here because the feature Konsist rule bans `Color.TRANSPARENT`. */
    const val TRANSPARENT: Int = 0

    /** Foreground slots of the hatch styles, in table order. */
    val hatchSlots: Array<PaletteSlot> =
        arrayOf(PaletteSlot.CatSleepFg, PaletteSlot.CatDarkFg, PaletteSlot.CatCaffeineFg, PaletteSlot.CatTravelFg)

    /** Group alphas a tile can have, in table order. */
    val hatchAlphas: DoubleArray = doubleArrayOf(1.0, PlanMetrics.optionalOpacity, PlanMetrics.markedOpacity)

    fun withAlpha(
        color: Int,
        alpha: Double,
    ): Int = (color and 0x00FFFFFF) or ((alpha * 255).roundToInt().coerceIn(0, 255) shl 24)

    /** `round(alpha * c + (1 - alpha) * under)` per sRGB channel, opaque. */
    fun precomposite(
        color: Int,
        alpha: Double,
        under: Int,
    ): Int {
        if (alpha >= 1.0) return color
        return (0xFF shl 24) or
            (mix(color, under, alpha, 16) shl 16) or
            (mix(color, under, alpha, 8) shl 8) or
            mix(color, under, alpha, 0)
    }

    private fun mix(
        color: Int,
        under: Int,
        alpha: Double,
        shift: Int,
    ): Int {
        val c = (color shr shift) and 0xFF
        val u = (under shr shift) and 0xFF
        return (alpha * c + (1 - alpha) * u).roundToInt().coerceIn(0, 255)
    }

    /** Any status, including an unknown one, marks a tile (iOS `block.status != nil`). */
    fun groupAlpha(
        status: CheckStatus?,
        priority: Priority,
    ): Double =
        when {
            status != null -> PlanMetrics.markedOpacity
            priority == Priority.Optional -> PlanMetrics.optionalOpacity
            else -> 1.0
        }

    fun hatchIndex(
        slot: PaletteSlot,
        alpha: Double,
    ): Int {
        val s = hatchSlots.indexOf(slot)
        require(s >= 0) { "$slot is not a hatch foreground" }
        val a = hatchAlphas.indexOfFirst { it == alpha }
        require(a >= 0) { "$alpha is not a group alpha" }
        return s * hatchAlphas.size + a
    }

    /** 12 entries (slot x alpha): the hatch colour pre-composited over `surface2`, then at the hatch opacity. */
    fun hatchColorTable(palette: TimelinePalette): IntArray {
        val under = palette.surface2
        return IntArray(hatchSlots.size * hatchAlphas.size) { index ->
            val slot = hatchSlots[index / hatchAlphas.size]
            val alpha = hatchAlphas[index % hatchAlphas.size]
            withAlpha(precomposite(palette.color(slot), alpha, under), PlanMetrics.hatchOpacity)
        }
    }
}
