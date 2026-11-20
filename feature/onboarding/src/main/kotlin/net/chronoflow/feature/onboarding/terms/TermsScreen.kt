package net.chronoflow.feature.onboarding.terms

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.chronoflow.core.designsystem.component.CfBanner
import net.chronoflow.core.designsystem.component.CfButton
import net.chronoflow.core.designsystem.component.CfButtonSize
import net.chronoflow.core.designsystem.component.CfButtonStyle
import net.chronoflow.core.designsystem.effect.cfClickable
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.generated.TermsTokens
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.layout.LocalSafeInsets
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.ui.R
import net.chronoflow.core.ui.links.PolicyLinks
import net.chronoflow.feature.onboarding.OnboardingInsets
import net.chronoflow.feature.onboarding.OnboardingMetrics

@Composable
fun TermsRoute(
    model: TermsViewModel,
    signOutConfirmation: @Composable (Int, () -> Unit, () -> Unit) -> Unit,
) {
    val state by model.state.collectAsStateWithLifecycle()
    TermsScreen(state, model::onEvent, signOutConfirmation = signOutConfirmation)
}

@Composable
fun TermsScreen(
    state: TermsUiState?,
    onEvent: (TermsEvent) -> Unit,
    modifier: Modifier = Modifier,
    signOutConfirmation: @Composable (Int, () -> Unit, () -> Unit) -> Unit = { _, _, _ -> },
) {
    val colors = CfTheme.colors
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .then(if (state != null) Modifier.testTag(TermsTags.SCREEN) else Modifier),
    ) {
        if (state != null) {
            val safe = LocalSafeInsets.current
            val density = LocalDensity.current
            val direction = LocalLayoutDirection.current
            val insets =
                with(density) {
                    OnboardingInsets(
                        safe.getLeft(this, direction).toDp().value,
                        safe.getTop(this).toDp().value,
                        safe.getRight(this, direction).toDp().value,
                        safe.getBottom(this).toDp().value,
                    )
                }
            val geometry = TermsGeometry(maxWidth.value, maxHeight.value, insets)
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = geometry.contentLeft.dp,
                        top = geometry.top.dp,
                        end = (maxWidth.value - geometry.contentLeft - geometry.contentWidth).dp,
                        bottom = geometry.bottom.dp,
                    ),
            ) {
                Column(
                    Modifier.fillMaxWidth().heightIn(min = geometry.contentHeight.dp).testTag(TermsTags.CONTENT),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(PlanUITokens.space5.dp)) {
                        Row(
                            Modifier.heightIn(min = AuthTokens.touch.dp),
                            horizontalArrangement = Arrangement.spacedBy(AuthTokens.bannerGap.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Image(
                                painterResource(net.chronoflow.feature.onboarding.R.drawable.terms_brand),
                                null,
                                Modifier.size(TermsTokens.brandSize.dp),
                            )
                            Text(
                                stringResource(
                                    R.string.terms_before_you_continue,
                                ).uppercase(LocalConfiguration.current.locales[0]),
                                color = colors.dawnText,
                                style = CfTheme.type.style(CfTypeScale.screenEyebrow),
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp)) {
                            Text(
                                stringResource(
                                    R.string.terms_we_ve_updated_our_terms,
                                ),
                                Modifier.testTag(TermsTags.TITLE).semantics {
                                    heading()
                                },
                                color = colors.foreground,
                                style = CfTheme.type.style(CfTypeScale.termsTitle),
                            )
                            Text(
                                stringResource(R.string.terms_please_take_a_moment_to_read_what_c_52b3c0),
                                color = colors.mutedForeground,
                                style = CfTheme.type.style(CfTypeScale.screenBodyLg),
                            )
                        }
                        TermsSummary()
                    }
                    Column(
                        Modifier.padding(top = (PlanUITokens.space5 + PlanUITokens.space5).dp),
                        verticalArrangement = Arrangement.spacedBy(PlanUITokens.space5.dp),
                    ) {
                        if (state.failed) {
                            CfBanner(
                                stringResource(R.string.terms_couldn_t_save_your_answer),
                                stringResource(R.string.terms_check_your_connection_and_try_again_14d498),
                                testTag = TermsTags.FAILURE,
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp)) {
                            if (state.signOutFailed) {
                                Text(
                                    stringResource(R.string.terms_something_went_wrong),
                                    Modifier.testTag(TermsTags.SIGN_OUT_FAILURE),
                                    color = colors.destructive,
                                    style = CfTheme.type.screenBody,
                                )
                            }
                            CfButton(
                                stringResource(
                                    if (state.failed) R.string.terms_try_again else R.string.terms_accept_and_continue,
                                ),
                                { onEvent(TermsEvent.Accept) },
                                Modifier.testTag(TermsTags.ACCEPT),
                                size = CfButtonSize.Lg,
                                fillWidth = true,
                                minHeight = PlanUITokens.actionMinHeight.dp,
                                enabled = state.acceptEnabled,
                            )
                            CfButton(
                                stringResource(R.string.terms_sign_out),
                                { onEvent(TermsEvent.SignOut) },
                                Modifier.testTag(TermsTags.SIGN_OUT),
                                style = CfButtonStyle.Ghost,
                                size = CfButtonSize.Lg,
                                fillWidth = true,
                                minHeight = PlanUITokens.actionMinHeight.dp,
                                enabled = state.signOutEnabled,
                            )
                            Text(
                                stringResource(R.string.terms_your_trips_stay_saved_if_you_sign_out),
                                Modifier.fillMaxWidth().padding(top = PlanUITokens.space1.dp),
                                color = colors.mutedForeground,
                                style = CfTheme.type.style(CfTypeScale.screenMeta),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
            state.pendingSignOutCount?.let { count ->
                signOutConfirmation(count, { onEvent(TermsEvent.Stay) }, { onEvent(TermsEvent.Discard) })
            }
        }
    }
}

@Composable
private fun TermsSummary() {
    val colors = CfTheme.colors
    val context = LocalContext.current
    val shape = RoundedCornerShape(PlanUITokens.space4.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .background(
                colors.surface2,
                RoundedCornerShape(PlanUITokens.radius2Xl.dp),
            ).padding(TermsTokens.summaryPadding.dp),
        verticalArrangement = Arrangement.spacedBy(PlanUITokens.space3.dp),
    ) {
        Text(
            stringResource(
                R.string.terms_what_changed,
            ).uppercase(LocalConfiguration.current.locales[0]),
            Modifier.semantics {
                heading()
            },
            color = colors.mutedForeground,
            style = CfTheme.type.style(CfTypeScale.screenLabel),
        )
        TermsBullet(R.string.terms_chronoflow_helps_you_plan_sleep_lig_30819d)
        TermsBullet(R.string.terms_chronoflow_gives_general_timing_gui_de4deb)
        TermsBullet(R.string.terms_your_account_stores_your_trips_prof_64f9bd)
        Row(
            Modifier
                .padding(top = PlanUITokens.space1.dp)
                .fillMaxWidth()
                .heightIn(min = TermsTokens.policyHeight.dp)
                .testTag(TermsTags.POLICY)
                .cfClickable({
                    PolicyLinks.open(context, PolicyLinks.TERMS, colors.isDark)
                }, role = Role.Button, focusShape = shape)
                .background(
                    colors.card,
                    shape,
                ).padding(horizontal = AuthTokens.bannerHorizontal.dp, vertical = PlanUITokens.space2.dp),
            horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                LucideGlyphs.FileText.imageVector(),
                null,
                Modifier.size(PlanUITokens.space5.dp),
                tint = colors.foreground,
            )
            Text(
                stringResource(R.string.terms_read_the_full_terms_and_privacy_policy),
                Modifier.weight(1f),
                color = colors.foreground,
                style = CfTheme.type.style(CfTypeScale.termsBody),
            )
            Icon(
                LucideGlyphs.ChevronRight.imageVector(),
                null,
                Modifier.size(OnboardingMetrics.POLICY_CHEVRON.dp),
                tint = colors.mutedForeground,
            )
        }
    }
}

@Composable
private fun TermsBullet(text: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space3.dp), verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .padding(
                    top = PlanUITokens.space2.dp,
                ).size(TermsTokens.bulletSize.dp)
                .background(CfTheme.colors.dawn, CircleShape)
                .clearAndSetSemantics {
                },
        )
        Text(
            stringResource(text),
            Modifier.weight(1f),
            color = CfTheme.colors.foreground,
            style = CfTheme.type.style(CfTypeScale.termsStrong),
        )
    }
}
