package net.chronoflow.feature.launch.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.component.CfButton
import net.chronoflow.core.designsystem.component.CfButtonStyle
import net.chronoflow.core.designsystem.effect.CfSky
import net.chronoflow.core.designsystem.generated.WelcomeTokens
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.designsystem.theme.LocalReduceMotion
import net.chronoflow.core.ui.R
import net.chronoflow.feature.launch.model.LaunchIntro
import net.chronoflow.feature.launch.model.MoonAnchorStore
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun WelcomeScreen(
    intro: LaunchIntro?,
    moonAnchorStore: MoonAnchorStore?,
    onPlanMyTrip: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduceMotion = LocalReduceMotion.current
    val presented = remember(intro) { intro?.takeIf { !it.hasPlayed && !reduceMotion } }
    val onComposition = welcomeCompositionObserver()
    SideEffect {
        presented?.claim()
        onComposition()
    }
    val now =
        remember(presented) {
            mutableLongStateOf((presented?.frozenTime ?: presented?.clock?.now)?.inWholeNanoseconds ?: 0L)
        }
    LaunchedEffect(presented) {
        val active = presented ?: return@LaunchedEffect
        if (active.frozenTime != null) return@LaunchedEffect
        active.clock.sleepUntil(active.start)
        while (true) {
            var elapsed = active.clock.now
            withFrameNanos {
                elapsed = active.clock.now
                now.longValue = elapsed.inWholeNanoseconds
            }
            if (elapsed >= active.start + 900.milliseconds) break
        }
    }
    val sky = CfSky.brush(CfTheme.colors.isDark)
    val locale = LocalConfiguration.current.locales[0]
    Box(modifier.fillMaxSize().background(sky)) {
        WelcomeColumn(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            heading = {
                Column(verticalArrangement = Arrangement.spacedBy(WelcomeTokens.groupGap.dp)) {
                    WelcomeBrand(moonAnchorStore, Modifier.introItem(0, presented, now))
                    Box(Modifier.padding(top = WelcomeTokens.eyebrowMargin.dp)) {
                        Text(
                            stringResource(R.string.welcome_jet_lag_planner).uppercase(locale),
                            Modifier.introItem(1, presented, now).testTag(WelcomeTags.EYEBROW),
                            color = CfTheme.colors.dawn,
                            style = CfTheme.type.eyebrow,
                        )
                    }
                    Text(
                        stringResource(
                            R.string.welcome_plan_your_clock_shift_for_your_next_trip,
                        ),
                        Modifier
                            .introItem(2, presented, now)
                            .testTag(WelcomeTags.HEADLINE)
                            .semantics {
                                heading()
                            }.welcomeLineHeight(CfTheme.type.headline.lineHeight),
                        color = CfTheme.colors.skyTitle,
                        style = CfTheme.type.headline,
                    )
                }
            },
            actions = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(WelcomeTokens.actionGap.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.fillMaxWidth().padding(bottom = WelcomeTokens.actionGap.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            stringResource(R.string.welcome_tell_us_your_flights_and_how_you_3cbe87),
                            Modifier
                                .widthIn(
                                    max = WelcomeWindowPolicy.COPY_CARD_MAX_DP.dp,
                                ).fillMaxWidth()
                                .introItem(3, presented, now)
                                .testTag(WelcomeTags.COPY)
                                .background(
                                    CfTheme.colors.skyCopyBackground,
                                    RoundedCornerShape(WelcomeTokens.cardRadius.dp),
                                ).padding(
                                    horizontal = WelcomeTokens.cardHorizontalPadding.dp,
                                    vertical = WelcomeTokens.cardVerticalPadding.dp,
                                ),
                            color = CfTheme.colors.skyCopy,
                            style = CfTheme.type.copy,
                        )
                    }
                    CfButton(
                        text = stringResource(R.string.welcome_plan_my_trip),
                        onClick = onPlanMyTrip,
                        modifier = Modifier.introItem(4, presented, now).testTag(WelcomeTags.PLAN),
                        style = CfButtonStyle.Dawn,
                        fillWidth = true,
                        minHeight = WelcomeTokens.buttonHeight.dp,
                        textStyle = CfTheme.type.primaryButton,
                        contentPadding =
                            PaddingValues(
                                horizontal = WelcomeTokens.cardHorizontalPadding.dp,
                                vertical = WelcomeTokens.brandGap.dp,
                            ),
                    )
                    CfButton(
                        text = stringResource(R.string.welcome_sign_in),
                        onClick = onSignIn,
                        modifier = Modifier.introItem(5, presented, now).testTag(WelcomeTags.SIGN_IN),
                        style = CfButtonStyle.OnSky,
                        fillWidth = true,
                        minHeight = WelcomeTokens.buttonHeight.dp,
                        textStyle = CfTheme.type.secondaryButton,
                        contentPadding =
                            PaddingValues(
                                horizontal = WelcomeTokens.cardHorizontalPadding.dp,
                                vertical = WelcomeTokens.brandGap.dp,
                            ),
                    )
                }
            },
        )
    }
}
