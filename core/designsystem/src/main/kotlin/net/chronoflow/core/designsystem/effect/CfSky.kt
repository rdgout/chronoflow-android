package net.chronoflow.core.designsystem.effect

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import net.chronoflow.core.designsystem.generated.SkyRamp

object CfSky {
    private val light = Brush.verticalGradient(*SkyRamp.light.map { it.location to it.color }.toTypedArray())
    private val dark = Brush.verticalGradient(*SkyRamp.dark.map { it.location to it.color }.toTypedArray())

    fun brush(dark: Boolean): Brush = if (dark) this.dark else light

    fun colorAt(
        dark: Boolean,
        t: Float,
    ): Color {
        val stops = if (dark) SkyRamp.dark else SkyRamp.light
        val position = t.coerceIn(0f, 1f)
        val index = stops.indexOfFirst { it.location >= position }
        if (index <= 0) return stops.first().color
        val a = stops[index - 1]
        val b = stops[index]
        val f = (position - a.location) / (b.location - a.location)
        return Color(
            a.color.red + (b.color.red - a.color.red) * f,
            a.color.green + (b.color.green - a.color.green) * f,
            a.color.blue + (b.color.blue - a.color.blue) * f,
        )
    }
}

fun Modifier.skyBackground(dark: Boolean): Modifier = drawBehind { drawRect(CfSky.brush(dark)) }
