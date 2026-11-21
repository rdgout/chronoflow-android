package net.chronoflow.feature.settings.bodyclock

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.withContext
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.designsystem.theme.LocalReduceMotion
import net.chronoflow.core.ui.R
import net.chronoflow.feature.settings.components.SettingsError
import net.chronoflow.feature.settings.components.SettingsLazyPage
import net.chronoflow.feature.settings.components.SettingsReadinessView
import net.chronoflow.feature.settings.components.SettingsStoredState
import kotlin.coroutines.EmptyCoroutineContext

data class BodyClockScreenState(
    val ui: BodyClockUiState,
    val rating: TextFieldState,
)

private object InstantSettingsScroll : MotionDurationScale {
    override val scaleFactor = 0f
}

@Composable
fun BodyClockScreen(
    state: BodyClockScreenState,
    onEvent: (BodyClockEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val list = rememberLazyListState()
    val requesters =
        remember {
            BodyClockGroup.entries.flatMap { it.fields }.associateWith { BringIntoViewRequester() }
        }
    val current by rememberUpdatedState(state.ui)
    val event by rememberUpdatedState(onEvent)
    val reduce by rememberUpdatedState(LocalReduceMotion.current)
    LaunchedEffect(list) {
        snapshotFlow {
            current.scroll?.takeIf { current.editor != null }?.let { it to (current.readiness != null) }
        }.filterNotNull()
            .collectLatest { (request, hasReadiness) ->
                val index = 1 + (if (hasReadiness) 1 else 0) + request.group.ordinal
                if (reduce) list.scrollToItem(index) else list.animateScrollToItem(index)
                request.fieldKey?.let { field ->
                    withFrameNanos { }
                    withContext(if (reduce) InstantSettingsScroll else EmptyCoroutineContext) {
                        requesters[field]?.bringIntoView()
                    }
                }
                event(BodyClockEvent.ScrollHandled(request.id))
            }
    }
    SettingsLazyPage(
        R.string.settings_body_clock,
        { onEvent(BodyClockEvent.Back) },
        "settings.bodyClock.screen",
        list,
        modifier,
    ) {
        if (state.ui.editor == null) {
            item("stored", "stored") {
                SettingsStoredState(
                    state.ui.pages.settingsLoaded,
                    state.ui.pages.failed,
                    { onEvent(BodyClockEvent.Retry) },
                )
            }
        } else {
            item("intro", "intro") {
                Text(
                    stringResource(R.string.settings_every_plan_is_built_from_these_a_0270a6),
                    Modifier.testTag("settings.bodyClock.intro"),
                    style = CfTheme.type.style(CfTypeScale.screenBodyLg),
                    color = CfTheme.colors.mutedForeground,
                )
            }
            state.ui.readiness?.let { readiness ->
                item("readiness", "readiness") {
                    SettingsReadinessView(readiness, { onEvent(BodyClockEvent.FirstMissing) })
                }
            }
            BodyClockGroup.entries.forEach { group ->
                item(group.raw, group.raw) {
                    BodyClockSection(state.ui.sections.getValue(group), state.rating, requesters, onEvent)
                }
            }
            if (state.ui.editor.failure) {
                item("failure", "failure") {
                    SettingsError(
                        stringResource(R.string.settings_we_couldn_t_save_your_change_ple_354da2),
                        Modifier.testTag("settings.bodyClock.failure"),
                    )
                }
            }
        }
    }
    state.ui.timeSheet?.let { BodyClockTimeSheet(it, onEvent) }
}

@Composable
fun BodyClockRoute(
    viewModel: BodyClockViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.ui.collectAsStateWithLifecycle()
    val rating =
        rememberBodyClockRatingField(
            viewModel,
            state.editor
                ?.answers
                ?.get("blueBlockerMdfd")
                .stringValue()
                .orEmpty(),
            state.editor != null,
        )
    val event: (BodyClockEvent) -> Unit =
        remember(viewModel, onBack) {
            { if (it == BodyClockEvent.Back) onBack() else viewModel.event(it) }
        }
    BodyClockScreen(BodyClockScreenState(state, rating), event, modifier)
}
