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
import net.chronoflow.core.ui.R
import net.chronoflow.feature.launch.model.LaunchIntro
import net.chronoflow.feature.launch.model.MoonAnchorStore

@Composable
@Suppress("UNUSED_PARAMETER")
fun WelcomeScreen(
    intro: LaunchIntro?,
    moonAnchorStore: MoonAnchorStore?,
    onPlanMyTrip: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sky = CfSky.brush(CfTheme.colors.isDark)
    val locale = LocalConfiguration.current.locales[0]
    Box(modifier.fillMaxSize().background(sky)) {
        WelcomeColumn(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            heading = {
                Column(verticalArrangement = Arrangement.spacedBy(WelcomeTokens.groupGap.dp)) {
                    WelcomeBrand(moonAnchorStore)
                    Box(Modifier.padding(top = WelcomeTokens.eyebrowMargin.dp)) {
                        Text(
                            stringResource(R.string.welcome_jet_lag_planner).uppercase(locale),
                            Modifier.testTag(WelcomeTags.EYEBROW).introItem(),
                            color = CfTheme.colors.dawn,
                            style = CfTheme.type.eyebrow,
                        )
                    }
                    Text(
                        stringResource(
                            R.string.welcome_plan_your_clock_shift_for_your_next_trip,
                        ),
                        Modifier
                            .testTag(WelcomeTags.HEADLINE)
                            .semantics {
                                heading()
                            }.introItem()
                            .welcomeLineHeight(CfTheme.type.headline.lineHeight),
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
                                .testTag(
                                    WelcomeTags.COPY,
                                ).introItem()
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
                        modifier = Modifier.testTag(WelcomeTags.PLAN).introItem(),
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
                        modifier = Modifier.testTag(WelcomeTags.SIGN_IN).introItem(),
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
