package net.chronoflow.feature.launch

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.testTag
import kotlinx.coroutines.flow.first
import net.chronoflow.core.ui.R
import net.chronoflow.feature.launch.model.LaunchFrame
import net.chronoflow.feature.launch.model.LaunchLayout
import net.chronoflow.feature.launch.model.LaunchMetrics
import net.chronoflow.feature.launch.model.LaunchSize
import net.chronoflow.feature.launch.model.StatusLineLayout

@Composable
fun LaunchOverlay(
    controller: LaunchController,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val fontScale = LocalDensity.current.fontScale
    val moon =
        remember(controller, density) {
            LaunchTrace.moonDecode { controller.moon(controller.appearance.resources(context)) }
        }
    val elapsed =
        remember(controller) {
            mutableLongStateOf(
                controller.overrides.freezeAt?.inWholeNanoseconds ?: if (controller.t0Nanos == LaunchController.UNSET) {
                    0L
                } else {
                    (controller.nanoTime() - controller.t0Nanos).coerceAtLeast(0L)
                },
            )
        }
    val description = stringResource(R.string.launch_chronoflow_is_opening)
    SideEffect { controller.compositionObserver() }
    val status = stringResource(R.string.launch_getting_your_plan_ready)
    val showsStatus = controller.showsStatus
    LaunchedEffect(controller, status, density, fontScale) {
        controller.configureStatus(
            context,
            status,
            density,
            fontScale,
            controller.appearance.colors.skyTitle
                .toArgb(),
        )
    }
    LaunchedEffect(controller) {
        controller.awaitFirstDraw()
        if (controller.overrides.freezeAt != null) return@LaunchedEffect
        if (controller.reduceMotion) controller.onOpeningSettled()
        runLaunchOpening(
            firstDraw = controller.firstDrawSignal,
            nanoTime = controller.nanoTime,
            nextFrameNanos = controller.nextFrameNanos,
            settledNanos = 0L,
            onElapsed = {
                elapsed.longValue = it
                if (it >= 1_500_000_000L) controller.onOpeningSettled()
            },
            planProvider = { controller.handOff },
            awaitRevision = { revision -> snapshotFlow { controller.handOff.revision }.first { it != revision } },
            onExit = controller::onExitFrame,
        )
    }

    Box(
        modifier
            .fillMaxSize()
            .onPlaced { controller.markOverlayPlaced() }
            .clearAndSetSemantics {
                contentDescription = description
                paneTitle = description
                role = Role.Image
                liveRegion = LiveRegionMode.Polite
                if (showsStatus) stateDescription = status
                testTag =
                    "launch.opening"
            }.pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent().changes.forEach { it.consume() }
                    }
                }
            }.graphicsLayer {
                val seconds = elapsed.longValue / 1e9
                val sampler = controller.handOff.sampler
                alpha = sampler.containerOpacity(seconds).toFloat()
                scaleX = sampler.containerScale(seconds).toFloat()
                scaleY = scaleX
                transformOrigin = TransformOrigin(0.5f, 0.4f)
            }.clipToBounds()
            .drawWithCache {
                val painter = LaunchScenePainter(size, density, controller.appearance, controller.reduceMotion, moon)
                Snapshot.withoutReadObservation {
                    controller.setOpening(
                        LaunchSize((size.width / density).toDouble(), (size.height / density).toDouble()),
                    )
                }
                onDrawBehind {
                    if (controller.t0Nanos == LaunchController.UNSET) controller.onFirstDraw(painter.moonRect)
                    painter.sampler = controller.handOff.sampler
                    val e = elapsed.longValue
                    painter.draw(this, e / 1e9)
                    controller.frameLog?.record(e, controller.nanoTime() - controller.t0Nanos)
                }
            }.drawWithCache {
                val raster = controller.statusRaster
                val frame = LaunchFrame()
                val layout =
                    raster?.let {
                        StatusLineLayout(
                            (size.width / density).toDouble(),
                            it.textWidthDp,
                            it.ascentDp,
                            it.descentDp,
                            it.lineHeightDp,
                            density.toDouble(),
                        )
                    }
                val top =
                    LaunchLayout(
                        LaunchSize((size.width / density).toDouble(), (size.height / density).toDouble()),
                    ).status.y
                val textOrigin =
                    layout?.let { Offset((it.text.x * density).toFloat(), ((top + it.text.y) * density).toFloat()) }
                        ?: Offset.Zero
                val centers =
                    Array(3) { i ->
                        Offset(
                            (
                                (
                                    (layout?.dots?.minX ?: 0.0) + LaunchMetrics.STATUS_DOT_RADIUS +
                                        i * LaunchMetrics.STATUS_DOT_PITCH
                                ) *
                                    density
                            ).toFloat(),
                            ((top + (raster?.lineHeightDp ?: 0.0) / 2) * density).toFloat(),
                        )
                    }
                val radius = LaunchMetrics.STATUS_DOT_RADIUS * density
                val color = controller.appearance.colors.skyTitle
                onDrawBehind {
                    if (raster != null) {
                        controller.handOff.sampler.sampleInto(elapsed.longValue / 1e9, frame)
                        val alpha = frame.statusOpacity.toFloat()
                        drawImage(raster.image, textOrigin, alpha = alpha)
                        var i = 0
                        while (i < 3) {
                            drawCircle(color, radius, centers[i], alpha = alpha * frame.statusDotOpacity[i].toFloat())
                            i++
                        }
                    }
                }
            },
    )
}
