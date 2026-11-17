package net.chronoflow.feature.launch

import android.graphics.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.toPath
import net.chronoflow.core.designsystem.effect.CfSky
import net.chronoflow.core.designsystem.generated.LaunchPaint
import net.chronoflow.feature.launch.generated.LaunchWordmark
import net.chronoflow.feature.launch.model.FlightPath
import net.chronoflow.feature.launch.model.LaunchFrame
import net.chronoflow.feature.launch.model.LaunchFrameSampler
import net.chronoflow.feature.launch.model.LaunchLayout
import net.chronoflow.feature.launch.model.LaunchMetrics
import net.chronoflow.feature.launch.model.LaunchSize
import net.chronoflow.feature.launch.model.LaunchTimeline

internal class LaunchScenePainter(
    private val sizePx: Size,
    private val density: Float,
    appearance: LaunchAppearance,
    reduceMotion: Boolean,
    private val moon: ImageBitmap,
) {
    private val sceneSize = LaunchSize((sizePx.width / density).toDouble(), (sizePx.height / density).toDouble())
    private val layout = LaunchLayout(sceneSize)
    var sampler = LaunchFrameSampler(LaunchTimeline.opening(sceneSize, reduceMotion))
    private val frame = LaunchFrame()
    private val colors = appearance.colors
    private val sky = CfSky.brush(appearance.night)
    private val haloRadius = LaunchMetrics.HALO_RADIUS * density
    private val sunRadius = LaunchMetrics.SUN_RADIUS * density
    private val sunGradientRadius = LaunchMetrics.SUN_GRADIENT_RADIUS * density
    private val planeRadius = LaunchMetrics.PLANE_RADIUS * density
    private val halo = haloBrush(appearance, density)
    private val sun = sunBrush(appearance, density)
    private val plane =
        Brush.radialGradient(
            *Array(LaunchPaint.planeAlphas.size) { i ->
                val r = i * 0.5f
                i.toFloat() / (LaunchPaint.planeAlphas.size - 1) to
                    LaunchPaint.white.copy(
                        alpha = if (r <= LaunchMetrics.PLANE_DOT_RADIUS) 1f else LaunchPaint.planeAlphas[i],
                    )
            },
            center = Offset(planeRadius, planeRadius),
            radius = planeRadius,
        )
    private val bandTop = (layout.band.y * density).toFloat()
    private val bandSize = Size(sizePx.width, sizePx.height - bandTop)
    private val band =
        Brush.verticalGradient(
            *Array(LaunchMetrics.bandPositions.size) { i ->
                LaunchMetrics.bandPositions[i] to
                    colors.skyHorizon.copy(alpha = LaunchMetrics.bandAlphas[i])
            },
            startY = bandTop,
            endY = sizePx.height,
        )
    private val flight = FlightPath(sceneSize)
    private val path = flightPath(flight, density)
    private val measure = PathMeasure().apply { setPath(path, false) }
    private val segment = Path()
    private val stroke = Stroke(LaunchMetrics.PATH_WIDTH * density, cap = StrokeCap.Butt)
    private val pathColor = LaunchPaint.white.copy(alpha = LaunchMetrics.PATH_ALPHA)
    private val wordmark =
        PathParser().parsePathString(LaunchWordmark.PATH_DATA).toPath().apply {
            transform(Matrix().apply { scale(density, density) })
        }
    private val wordmarkLeft = ((sceneSize.width - LaunchWordmark.WIDTH_DP) / 2 * density).toFloat()
    private val wordmarkTop = ((layout.wordmarkCentre.y - LaunchWordmark.HEIGHT_DP / 2) * density).toFloat()
    private val sunX = (layout.sunCentre.x * density).toFloat()
    private val sunY = (layout.sunCentre.y * density).toFloat()
    private val moonLeft = (sizePx.width.toInt() - moon.width) / 2
    private val moonTop = (sizePx.height.toInt() - moon.height) / 2
    val moonRect = Rect(moonLeft, moonTop, moonLeft + moon.width, moonTop + moon.height)
    private val moonCentre = Offset(moonLeft + moon.width / 2f, moonTop + moon.height / 2f)
    private val haloLocal = Offset(haloRadius, haloRadius)
    private val sunLocal = Offset(sunGradientRadius, sunGradientRadius)
    private val planeLocal = Offset(planeRadius, planeRadius)

    fun draw(
        scope: DrawScope,
        seconds: Double,
    ) = with(scope) {
        sampler.sampleInto(seconds, frame)
        drawRect(colors.skyTop)
        if (frame.skyOpacity > 0) drawRect(sky, alpha = frame.skyOpacity.toFloat())
        if (frame.haloOpacity > 0) {
            translate(sunX - haloRadius, sunY + (frame.haloTranslationY * density).toFloat() - haloRadius) {
                drawCircle(halo, haloRadius, haloLocal, alpha = frame.haloOpacity.toFloat())
            }
        }
        if (frame.sunOpacity >
            0
        ) {
            translate(
                sunX - sunGradientRadius,
                sunY + (frame.sunTranslationY * density).toFloat() - sunGradientRadius,
            ) {
                drawCircle(sun, sunRadius, sunLocal, alpha = frame.sunOpacity.toFloat())
            }
        }
        if (frame.bandOpacity > 0) drawRect(band, Offset(0f, bandTop), bandSize, alpha = frame.bandOpacity.toFloat())
        if (frame.pathOpacity > 0 && frame.strokeEnd > 0) {
            segment.rewind()
            measure.getSegment(0f, (frame.strokeEnd * measure.length).toFloat(), segment, true)
            drawPath(segment, pathColor, alpha = frame.pathOpacity.toFloat(), style = stroke)
        }
        if (frame.planeOpacity >
            0
        ) {
            translate(
                (frame.planeX * density).toFloat() - planeRadius,
                (frame.planeY * density).toFloat() - planeRadius,
            ) {
                drawCircle(plane, planeRadius, planeLocal, alpha = frame.planeOpacity.toFloat())
            }
        }
        if (frame.moonOpacity > 0) {
            val dx = ((frame.moonX - sceneSize.width / 2) * density).toFloat()
            val dy = ((frame.moonY - sceneSize.height / 2) * density).toFloat()
            translate(dx, dy) {
                scale(frame.moonScale.toFloat(), pivot = moonCentre) {
                    drawImage(moon, Offset(moonLeft.toFloat(), moonTop.toFloat()), alpha = frame.moonOpacity.toFloat())
                }
            }
        }
        if (frame.wordmarkOpacity > 0) {
            translate(wordmarkLeft, wordmarkTop + (frame.wordmarkTranslationY * density).toFloat()) {
                drawPath(wordmark, colors.skyTitle, alpha = frame.wordmarkOpacity.toFloat())
            }
        }
    }

    companion object {
        fun flightPath(
            flight: FlightPath,
            density: Float,
        ): Path =
            Path().apply {
                flight.vertices.forEachIndexed { i, p ->
                    if (i == 0) {
                        moveTo((p.x * density).toFloat(), (p.y * density).toFloat())
                    } else {
                        lineTo((p.x * density).toFloat(), (p.y * density).toFloat())
                    }
                }
            }

        fun haloBrush(
            appearance: LaunchAppearance,
            density: Float,
        ): Brush {
            val radius = LaunchMetrics.HALO_RADIUS * density
            return Brush.radialGradient(
                *Array(LaunchPaint.haloPositions.size) { i ->
                    LaunchPaint.haloPositions[i] to
                        appearance.colors.dawn.copy(alpha = LaunchPaint.haloAlphas[i])
                },
                center = Offset(radius, radius),
                radius = radius,
            )
        }

        fun sunBrush(
            appearance: LaunchAppearance,
            density: Float,
        ): Brush {
            val radius = LaunchMetrics.SUN_GRADIENT_RADIUS * density
            val dawn = appearance.colors.dawn
            return Brush.radialGradient(
                0f to dawn,
                LaunchMetrics.SUN_SOLID_RADIUS / LaunchMetrics.SUN_GRADIENT_RADIUS to dawn,
                LaunchMetrics.SUN_CLEAR_RADIUS / LaunchMetrics.SUN_GRADIENT_RADIUS to dawn.copy(alpha = 0f),
                1f to dawn.copy(alpha = 0f),
                center = Offset(radius, radius),
                radius = radius,
            )
        }
    }
}
