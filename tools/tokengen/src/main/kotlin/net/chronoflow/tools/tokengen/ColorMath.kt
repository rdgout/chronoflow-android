package net.chronoflow.tools.tokengen

import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

class GenerationError(
    message: String,
) : Exception(message)

class RGB(
    val values: List<Double>,
) {
    val hex: String
        get() = "#" + values.joinToString("") { String.format(Locale.ROOT, "%02X", roundHalfUp(it * 255)) }

    fun mixed(
        with: RGB,
        fraction: Double,
    ): RGB = RGB(values.zip(with.values).map { (a, b) -> a + (b - a) * fraction })
}

/** Half up (Swift `rounded()` on positive values), never banker's rounding. */
fun roundHalfUp(value: Double): Long = Math.round(value)

class OKLab(
    val lightness: Double,
    val a: Double,
    val b: Double,
    val alpha: Double = 1.0,
) {
    fun mixed(
        with: OKLab,
        fraction: Double,
    ): OKLab =
        OKLab(
            lightness + (with.lightness - lightness) * fraction,
            a + (with.a - a) * fraction,
            b + (with.b - b) * fraction,
            alpha + (with.alpha - alpha) * fraction,
        )

    fun withAlpha(value: Double): OKLab = OKLab(lightness, a, b, value)

    fun rgb(p3: Boolean): RGB {
        val l = (lightness + 0.3963377774 * a + 0.2158037573 * b).pow(3.0)
        val m = (lightness - 0.1055613458 * a - 0.0638541728 * b).pow(3.0)
        val s = (lightness - 0.0894841775 * a - 1.2914855480 * b).pow(3.0)
        val r = 4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s
        val g = -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s
        val bl = -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s
        val linear =
            if (p3) {
                listOf(
                    0.8224619687 * r + 0.1775380313 * g,
                    0.0331941989 * r + 0.9668058011 * g,
                    0.0170826307 * r + 0.0723974407 * g + 0.9105199286 * bl,
                )
            } else {
                listOf(r, g, bl)
            }
        return RGB(
            linear.map {
                val clipped = min(1.0, max(0.0, it))
                if (clipped <= 0.0031308) 12.92 * clipped else 1.055 * clipped.pow(1 / 2.4) - 0.055
            },
        )
    }

    companion object {
        fun fromCss(css: String): OKLab {
            val parts =
                css
                    .replace("oklch(", "")
                    .replace(")", "")
                    .replace("/", " ")
                    .split(" ")
                    .filter { it.isNotEmpty() }
                    .mapNotNull { it.toDoubleOrNull() }
            if (parts.size != 3 && parts.size != 4) throw GenerationError("Invalid OKLCH: $css")
            return OKLab(
                parts[0],
                parts[1] * cos(parts[2] * PI / 180),
                parts[1] * sin(parts[2] * PI / 180),
                if (parts.size == 4) parts[3] else 1.0,
            )
        }
    }
}

class RampStop(
    val location: Double,
    val color: RGB,
)

/** The iOS `skyRamp`, checked in sRGB by default (OQ-A7); the tolerance stays 0.25/255 per channel. */
fun skyRamp(
    top: OKLab,
    mid: OKLab,
    horizon: OKLab,
    midStop: Double,
    p3: Boolean = false,
): List<RampStop> {
    fun segment(
        from: OKLab,
        to: OKLab,
        start: Double,
        end: Double,
        divisions: Int,
    ): List<RampStop> =
        (0..divisions).map { i ->
            val f = i.toDouble() / divisions
            RampStop(start + (end - start) * f, from.mixed(to, f).rgb(p3))
        }

    var divisions = 1
    while (true) {
        val stops =
            segment(top, mid, 0.0, midStop, divisions).dropLast(1) + segment(mid, horizon, midStop, 1.0, divisions)
        val valid =
            (0..1000).all { i ->
                val x = i.toDouble() / 1000
                val trueColor =
                    if (x <= midStop) {
                        top.mixed(mid, x / midStop).rgb(p3)
                    } else {
                        mid.mixed(horizon, (x - midStop) / (1 - midStop)).rgb(p3)
                    }
                val hi = stops.indexOfFirst { it.location >= x }.let { if (it < 0) stops.size - 1 else it }
                val lo = max(0, hi - 1)
                val f = if (hi == lo) 0.0 else (x - stops[lo].location) / (stops[hi].location - stops[lo].location)
                val approximation = stops[lo].color.mixed(stops[hi].color, f)
                approximation.values.zip(trueColor.values).all { (p, q) -> abs(p - q) <= 0.25 / 255.0 }
            }
        if (valid) return stops
        divisions *= 2
    }
}
