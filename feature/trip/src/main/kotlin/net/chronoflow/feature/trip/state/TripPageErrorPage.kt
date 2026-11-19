package net.chronoflow.feature.trip.state

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.component.CfButton
import net.chronoflow.core.designsystem.component.CfButtonSize
import net.chronoflow.core.designsystem.component.CfButtonStyle
import net.chronoflow.core.designsystem.effect.skyBackground
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.layout.LocalSafeInsets
import net.chronoflow.core.designsystem.system.CfBackButton
import net.chronoflow.core.designsystem.system.CfCenterAlignedTopAppBar
import net.chronoflow.core.designsystem.system.CfPullToRefreshBox
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.plan.generated.PlanMetrics
import net.chronoflow.core.plan.trippage.TripPageState
import net.chronoflow.core.ui.R

@Composable
fun TripPageErrorPage(
    state: TripPageState,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    onPrimary: () -> Unit,
    onToday: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val missing = state == TripPageState.NotFound
    val eyebrow = if (missing) R.string.plan_not_found else R.string.plan_something_went_wrong
    val title = if (missing) R.string.plan_we_cant_find_that else R.string.plan_were_having_trouble
    val body =
        if (missing) {
            R.string.plan_the_link_may_be_old_or_the_trip_was_deleted
        } else {
            R.string.plan_its_on_our_side_not_yours_and_your_p_b30437
        }
    val primary = if (missing) R.string.plan_go_to_my_trips else R.string.plan_try_again
    val secondary = if (missing) R.string.plan_open_today else R.string.plan_open_my_plan_for_today
    val colors = CfTheme.colors
    val safe = LocalSafeInsets.current
    val density = LocalDensity.current
    val bottom = with(density) { safe.getBottom(this).toDp() }
    Column(
        modifier
            .fillMaxSize()
            .skyBackground(
                colors.isDark,
            ).testTag(if (missing) "trip.error.notFound" else "trip.error.server"),
    ) {
        CfCenterAlignedTopAppBar(title = {}, navigationIcon = {
            CfBackButton(stringResource(R.string.screens_my_trips), onBack, Modifier.testTag("trip.back"), onSky = true)
        }, onSky = true)
        CfPullToRefreshBox(refreshing, onRefresh, Modifier.weight(1f).fillMaxWidth()) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val viewport = maxHeight
                Column(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(
                            rememberScrollState(),
                        ).heightIn(min = viewport)
                        .testTag("trip.scroll"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.weight(1f))
                    Column(
                        Modifier.fillMaxWidth().padding(PlanMetrics.gutter.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(PlanMetrics.sectionGap.dp),
                    ) {
                        Text(
                            stringResource(eyebrow),
                            style = CfTheme.type.eyebrow,
                            color = colors.skyTitle,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            stringResource(title),
                            Modifier.testTag("trip.error.title").semantics {
                                heading()
                            },
                            style = CfTheme.type.style(CfTypeScale.authHeading),
                            color = colors.skyTitle,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            stringResource(body),
                            style = CfTheme.type.authBody,
                            color = colors.skyTitle,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Column(
                        Modifier.fillMaxWidth().padding(
                            start = PlanMetrics.errorPageHorizontal.dp,
                            end = PlanMetrics.errorPageHorizontal.dp,
                            bottom =
                                PlanMetrics.errorPageBottom.dp + bottom,
                        ),
                        verticalArrangement = Arrangement.spacedBy(PlanMetrics.rowGap.dp),
                    ) {
                        CfButton(
                            stringResource(primary),
                            onPrimary,
                            Modifier.testTag("trip.error.primary"),
                            style = CfButtonStyle.Dawn,
                            size = CfButtonSize.Lg,
                            fillWidth = true,
                        )
                        CfButton(
                            stringResource(secondary),
                            { onToday?.invoke() },
                            Modifier.testTag("trip.error.secondary"),
                            style = CfButtonStyle.OnSky,
                            size = CfButtonSize.Touch,
                            fillWidth = true,
                            enabled =
                                onToday != null,
                        )
                    }
                }
            }
        }
    }
}
