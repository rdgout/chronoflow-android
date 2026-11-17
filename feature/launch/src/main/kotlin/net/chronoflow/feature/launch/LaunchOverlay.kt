package net.chronoflow.feature.launch

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.testTag
import net.chronoflow.core.ui.R
import net.chronoflow.feature.launch.model.LaunchFrameSampler
import net.chronoflow.feature.launch.model.LaunchSize
import net.chronoflow.feature.launch.model.LaunchTimeline

private class SceneCache {
    var painter: LaunchScenePainter? = null
}

@Composable
fun LaunchOverlay(
    controller: LaunchController,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val moon =
        remember(controller, density) {
            LaunchTrace.moonDecode { controller.moon(controller.appearance.resources(context)) }
        }
    val elapsed = remember(controller) { mutableLongStateOf(controller.overrides.freezeAt?.inWholeNanoseconds ?: 0L) }
    val scene = remember(controller) { SceneCache() }
    val opening =
        remember(
            controller,
        ) { LaunchFrameSampler(LaunchTimeline.opening(LaunchSize(1.0, 1.0), controller.reduceMotion)) }
    val description = stringResource(R.string.launch_chronoflow_is_opening)
    SideEffect { controller.compositionObserver() }
    LaunchedEffect(controller) {
        if (controller.reduceMotion || controller.overrides.freezeAt != null) {
            controller.onOpeningSettled()
        } else {
            val settled = (opening.settledAfterSeconds * 1e9).toLong()
            while (elapsed.longValue < settled) {
                withFrameNanos { nanos ->
                    elapsed.longValue =
                        if (controller.t0Nanos ==
                            LaunchController.UNSET
                        ) {
                            0L
                        } else {
                            (nanos - controller.t0Nanos).coerceAtLeast(0L)
                        }
                }
            }
            controller.onOpeningSettled()
        }
    }
    Box(
        modifier
            .fillMaxSize()
            .onPlaced { controller.markOverlayPlaced() }
            .clearAndSetSemantics {
                contentDescription = description
                paneTitle = description
                role = Role.Image
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
                val sampler = scene.painter?.sampler ?: opening
                alpha = sampler.containerOpacity(seconds).toFloat()
                scaleX = sampler.containerScale(seconds).toFloat()
                scaleY = scaleX
                transformOrigin = TransformOrigin(0.5f, 0.4f)
            }.clipToBounds()
            .drawWithCache {
                val painter = LaunchScenePainter(size, density, controller.appearance, controller.reduceMotion, moon)
                scene.painter = painter
                onDrawBehind {
                    if (controller.t0Nanos == LaunchController.UNSET) controller.onFirstDraw(painter.moonRect)
                    painter.draw(this, elapsed.longValue / 1e9)
                }
            },
    )
}
