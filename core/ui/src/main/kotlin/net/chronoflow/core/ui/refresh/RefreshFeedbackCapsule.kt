package net.chronoflow.core.ui.refresh

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.layout.LocalSafeInsets
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.sync.refresh.RefreshDebugProbe
import net.chronoflow.core.sync.refresh.RefreshFeedbackState

private object RefreshCapsuleMetrics {
    const val ANIMATION_MILLIS = 350
    const val PRESENTATION_MILLIS = 2000L
}

@Composable
fun RefreshFeedbackCapsule(
    state: RefreshFeedbackState,
    onDismiss: (Long) -> Unit,
    probe: RefreshDebugProbe,
    modifier: Modifier = Modifier,
) {
    val currentDismiss = rememberUpdatedState(onDismiss)
    val text = refreshFeedbackText(state.feedback)
    var lastText by remember { mutableStateOf(text) }
    if (text.isNotEmpty()) lastText = text
    val visible = state.feedback.isNotEmpty()
    if (visible) {
        LaunchedEffect(state.presentationId) {
            probe.presented(text)
            delay(RefreshCapsuleMetrics.PRESENTATION_MILLIS)
            currentDismiss.value(state.presentationId)
        }
    }
    val fadeIn = fadeIn(tween(RefreshCapsuleMetrics.ANIMATION_MILLIS))
    val fadeOut = fadeOut(tween(RefreshCapsuleMetrics.ANIMATION_MILLIS))
    AnimatedVisibility(
        visible,
        modifier
            .windowInsetsPadding(LocalSafeInsets.current.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = PlanUITokens.space4.dp, vertical = PlanUITokens.space2.dp),
        enter =
            if (CfTheme.reduceMotion) {
                fadeIn
            } else {
                fadeIn +
                    slideInVertically(tween(RefreshCapsuleMetrics.ANIMATION_MILLIS)) { -it }
            },
        exit =
            if (CfTheme.reduceMotion) {
                fadeOut
            } else {
                fadeOut +
                    slideOutVertically(tween(RefreshCapsuleMetrics.ANIMATION_MILLIS)) { -it }
            },
    ) {
        Text(
            lastText,
            Modifier
                .clearAndSetSemantics {
                    contentDescription = lastText
                    liveRegion = LiveRegionMode.Polite
                    testTag = "refresh.feedback"
                }.background(
                    CfTheme.colors.card,
                    CircleShape,
                ).border(AuthTokens.borderWidth.dp, CfTheme.colors.border, CircleShape)
                .padding(horizontal = PlanUITokens.space4.dp, vertical = PlanUITokens.space2.dp),
            color = CfTheme.colors.foreground,
            style = CfTheme.type.screenBody,
            textAlign = TextAlign.Center,
        )
    }
}
