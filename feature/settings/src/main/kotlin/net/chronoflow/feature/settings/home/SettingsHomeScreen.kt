package net.chronoflow.feature.settings.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.component.CfButton
import net.chronoflow.core.designsystem.component.CfButtonSize
import net.chronoflow.core.designsystem.component.CfButtonStyle
import net.chronoflow.core.designsystem.component.CfCard
import net.chronoflow.core.designsystem.component.CfDivider
import net.chronoflow.core.designsystem.effect.cfClickable
import net.chronoflow.core.designsystem.effect.softShadow
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.icon.LucideGlyph
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.layout.BoardMetrics
import net.chronoflow.core.designsystem.semantics.ellipsizable
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.plan.PlanClock
import net.chronoflow.core.ui.R
import net.chronoflow.feature.settings.components.SettingsDestinationRow
import net.chronoflow.feature.settings.components.SettingsPage
import net.chronoflow.feature.settings.signout.SignOutPendingSheet
import java.time.ZoneId

@BoardMetrics("SetHome")
object SettingsHomeMetrics {
    const val PROFILE_HEIGHT = 72f
}

data class SettingsHomeScreenState(
    val ui: SettingsHomeUi,
    val clock: PlanClock,
    val zone: ZoneId,
    val formatter: CopyLineFormatter? = null,
    val showUnbuilt: Boolean = false,
)

sealed interface SettingsHomeEvent {
    data class Open(
        val route: Route,
    ) : SettingsHomeEvent

    data object SignOut : SettingsHomeEvent

    data object Stay : SettingsHomeEvent

    data object Discard : SettingsHomeEvent
}

enum class SettingsDestination(
    val route: Route,
    val glyph: LucideGlyph,
    val title: Int,
    val subtitle: Int?,
    val tag: String,
    val isBuilt: Boolean = false,
) {
    Account(
        Route.Account,
        LucideGlyphs.User,
        R.string.settings_account,
        R.string.settings_profile_language,
        "settings.account",
        true,
    ),
    Security(
        Route.Security,
        LucideGlyphs.Shield,
        R.string.settings_security,
        R.string.settings_password_two_factor,
        "settings.security",
        true,
    ),
    BodyClock(
        Route.BodyClock,
        LucideGlyphs.Moon,
        R.string.settings_body_clock,
        R.string.settings_the_answers_every_plan_is_built_from,
        "settings.bodyClock",
        true,
    ),
    Preferences(
        Route.Preferences,
        LucideGlyphs.SlidersHorizontal,
        R.string.settings_preferences,
        R.string.settings_appearance_timeline_reminders,
        "settings.preferences",
        true,
    ),
    Plan(Route.Plan, LucideGlyphs.CreditCard, R.string.settings_plan_credits, null, "settings.plan", true),
}

@Composable
fun SettingsHomeScreen(
    state: SettingsHomeScreenState,
    onEvent: (SettingsHomeEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsPage(R.string.settings_settings, null, "settings.screen", modifier) {
        state.ui.state.account?.let { account ->
            CfCard(
                Modifier
                    .testTag("settings.profile")
                    .softShadow(CfTheme.shapes.x2l, CfTheme.colors.isDark)
                    .cfClickable({ onEvent(SettingsHomeEvent.Open(Route.Profile)) }, focusShape = CfTheme.shapes.x2l),
                rows = true,
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = SettingsHomeMetrics.PROFILE_HEIGHT.dp)
                        .padding(horizontal = PlanUITokens.space4.dp, vertical = PlanUITokens.space3.dp),
                    horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(AuthTokens.touch.dp)
                            .background(CfTheme.colors.primary, CircleShape)
                            .clearAndSetSemantics {},
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            state.ui.state.initials,
                            style = CfTheme.type.style(CfTypeScale.settingsStrong),
                            color = CfTheme.colors.primaryForeground,
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(account.name, style = CfTheme.type.style(CfTypeScale.settingsStrong))
                        Text(
                            account.email,
                            Modifier.ellipsizable(),
                            style = CfTheme.type.screenBody,
                            color = CfTheme.colors.mutedForeground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Icon(
                        LucideGlyphs.ChevronRight.imageVector(),
                        null,
                        Modifier.size(PlanUITokens.space5.dp),
                        tint = CfTheme.colors.mutedForeground,
                    )
                }
            }
        }
        SettingsSyncLine(state.ui.state, state.ui.syncing, state.ui.offline, state.clock, state.zone, state.formatter)
        val destinations = SettingsDestination.entries.filter { it.isBuilt || state.showUnbuilt }
        CfCard(rows = true) {
            destinations.forEachIndexed { index, destination ->
                if (index > 0) CfDivider()
                val subtitle =
                    destination.subtitle?.let { stringResource(it) }
                        ?: state.ui.state.credits
                            ?.takeIf { destination == SettingsDestination.Plan }
                            ?.let { pluralStringResource(R.plurals.settings_n_credits_left, it, it) }
                SettingsDestinationRow(
                    stringResource(destination.title),
                    destination.glyph,
                    destination.tag,
                    { onEvent(SettingsHomeEvent.Open(destination.route)) },
                    subtitle = subtitle,
                    badge =
                        if (destination == SettingsDestination.BodyClock && state.ui.state.missingAnswers > 0) {
                            pluralStringResource(
                                R.plurals.settings_n_to_answer,
                                state.ui.state.missingAnswers,
                                state.ui.state.missingAnswers,
                            )
                        } else {
                            null
                        },
                    badgeTag = "settings.readiness",
                )
            }
        }
        val signOut = stringResource(R.string.settings_sign_out)
        CfButton(
            signOut,
            { onEvent(SettingsHomeEvent.SignOut) },
            Modifier.testTag("settings.signOut"),
            style = CfButtonStyle.Outline,
            size = CfButtonSize.Lg,
            fillWidth = true,
            enabled = !state.ui.busy && state.ui.state.account != null,
            loading = state.ui.busy,
            loadingDescription = signOut,
        )
    }
    if (state.ui.showsPending) {
        SignOutPendingSheet(
            state.ui.state.pending,
            state.ui.busy,
            { onEvent(SettingsHomeEvent.Stay) },
            { onEvent(SettingsHomeEvent.Discard) },
        )
    }
}
