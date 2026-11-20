package net.chronoflow.feature.onboarding.walkthrough

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.chronoflow.core.designsystem.component.CfButton
import net.chronoflow.core.designsystem.component.CfButtonSize
import net.chronoflow.core.designsystem.component.CfButtonStyle
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.layout.LocalSafeInsets
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.designsystem.theme.LocalReduceMotion
import net.chronoflow.core.ui.R
import net.chronoflow.feature.onboarding.OnboardingInsets

@Composable
fun WalkthroughRoute(
    model: WalkthroughViewModel,
    onFinish: () -> Unit = {},
) {
    val state by model.state.collectAsStateWithLifecycle()
    BackHandler(enabled = model.replay) { model.onEvent(WalkthroughEvent.Skip) }
    val finish by rememberUpdatedState(onFinish)
    LaunchedEffect(model, state?.completed) { if (model.takeFinish()) finish() }
    WalkthroughScreen(state, model::onEvent)
}

@Composable
fun WalkthroughScreen(
    state: WalkthroughUiState?,
    onEvent: (WalkthroughEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CfTheme.colors
    BoxWithConstraints(
        modifier.fillMaxSize().background(colors.background).then(
            if (state !=
                null
            ) {
                Modifier.testTag(WalkthroughTags.SCREEN)
            } else {
                Modifier
            },
        ),
    ) {
        if (state != null) {
            val density = LocalDensity.current
            val direction = LocalLayoutDirection.current
            val safe = LocalSafeInsets.current
            val insets =
                with(density) {
                    OnboardingInsets(
                        safe.getLeft(this, direction).toDp().value,
                        safe.getTop(this).toDp().value,
                        safe.getRight(this, direction).toDp().value,
                        safe.getBottom(this).toDp().value,
                    )
                }
            val geometry = WalkthroughGeometry(maxWidth.value, maxHeight.value, insets)
            Column(
                Modifier
                    .padding(
                        start = geometry.contentLeft.dp,
                        top = geometry.top.dp,
                        end = (maxWidth.value - geometry.contentLeft - geometry.contentWidth).dp,
                        bottom = geometry.bottom.dp,
                    ).fillMaxSize()
                    .testTag(WalkthroughTags.CONTENT),
            ) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = PlanUITokens.actionMinHeight.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(
                            R.string.walkthrough_current_of_total,
                            (state.page.ordinal + 1).toString(),
                            WalkthroughModel.Page.entries.size
                                .toString(),
                        ).uppercase(LocalConfiguration.current.locales[0]),
                        Modifier.padding(start = PlanUITokens.space1.dp).testTag(WalkthroughTags.COUNTER),
                        color = colors.dawnText,
                        style = CfTheme.type.style(CfTypeScale.screenEyebrow).copy(fontFeatureSettings = "tnum"),
                    )
                    Spacer(Modifier.weight(1f))
                    CfButton(
                        stringResource(R.string.walkthrough_skip),
                        { onEvent(WalkthroughEvent.Skip) },
                        Modifier.testTag(WalkthroughTags.SKIP),
                        style = CfButtonStyle.Ghost,
                        size = CfButtonSize.Sm,
                        minHeight = AuthTokens.touch.dp,
                        enabled = !state.busy && !state.completed,
                    )
                }
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                    val viewportHeight = maxHeight
                    key(state.page) {
                        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                            Column(
                                Modifier.fillMaxWidth().heightIn(min = viewportHeight),
                                verticalArrangement =
                                    Arrangement.spacedBy(
                                        PlanUITokens.walkthroughContentGap.dp,
                                        Alignment.CenterVertically,
                                    ),
                            ) {
                                WalkthroughPreview(state.page)
                                Column(
                                    Modifier.padding(horizontal = PlanUITokens.space1.dp),
                                    verticalArrangement = Arrangement.spacedBy(PlanUITokens.walkthroughCopyGap.dp),
                                ) {
                                    Text(
                                        stringResource(title(state.page)),
                                        Modifier.testTag(WalkthroughTags.TITLE).semantics {
                                            heading()
                                            liveRegion =
                                                LiveRegionMode.Polite
                                        },
                                        color = colors.foreground,
                                        style = CfTheme.type.style(CfTypeScale.screenPageTitle),
                                    )
                                    Text(
                                        stringResource(detail(state.page)),
                                        color = colors.mutedForeground,
                                        style = CfTheme.type.style(CfTypeScale.screenBodyLg),
                                    )
                                }
                            }
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(PlanUITokens.space5.dp)) {
                    Row(
                        Modifier.fillMaxWidth().testTag(WalkthroughTags.DOTS).clearAndSetSemantics {
                        },
                        horizontalArrangement =
                            Arrangement.spacedBy(
                                PlanUITokens.space2.dp,
                                Alignment.CenterHorizontally,
                            ),
                    ) {
                        WalkthroughModel.Page.entries.forEach { page -> WalkthroughDot(state.page == page) }
                    }
                    val done = state.page == WalkthroughModel.Page.Progress
                    CfButton(
                        stringResource(if (done) R.string.walkthrough_done else R.string.walkthrough_next),
                        { onEvent(WalkthroughEvent.Advance) },
                        Modifier.testTag(if (done) WalkthroughTags.DONE else WalkthroughTags.NEXT),
                        size = CfButtonSize.Lg,
                        fillWidth = true,
                        enabled = !state.busy && !state.completed,
                        minHeight = PlanUITokens.actionMinHeight.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun WalkthroughDot(active: Boolean) {
    val target = walkthroughDotWidth(active)
    val reduceMotion = LocalReduceMotion.current
    val width =
        if (reduceMotion) {
            rememberUpdatedState(target)
        } else {
            animateFloatAsState(
                target,
                walkthroughDotAnimationSpec(reduceMotion),
                label = "walkthrough.dot",
            )
        }
    Box(
        Modifier
            .layout { measurable, constraints ->
                val w = width.value.dp.roundToPx()
                val placeable = measurable.measure(constraints.copy(minWidth = w, maxWidth = w))
                layout(w, placeable.height) { placeable.place(0, 0) }
            }.height(
                PlanUITokens.space2.dp,
            ).background(
                if (active) CfTheme.colors.dawn else CfTheme.colors.border,
                RoundedCornerShape(PlanUITokens.radiusFull.dp),
            ),
    )
}

private fun title(page: WalkthroughModel.Page) =
    when (page) {
        WalkthroughModel.Page.Timeline -> R.string.walkthrough_your_plan_on_one_timeline
        WalkthroughModel.Page.Categories -> R.string.walkthrough_know_each_step_at_a_glance
        WalkthroughModel.Page.Progress -> R.string.walkthrough_tick_off_what_you_did
    }

private fun detail(page: WalkthroughModel.Page) =
    when (page) {
        WalkthroughModel.Page.Timeline -> R.string.walkthrough_sleep_light_caffeine_and_your_06bee8
        WalkthroughModel.Page.Categories -> R.string.walkthrough_every_kind_of_step_has_its_ow_14503c
        WalkthroughModel.Page.Progress -> R.string.walkthrough_checking_off_changes_your_pro_a36d57
    }
