package net.chronoflow.feature.trip.actions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.layout.LocalSafeInsets
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.plan.PageCopy
import net.chronoflow.core.plan.PageCopyTable
import net.chronoflow.core.plan.actions.TripActionsAvailability
import net.chronoflow.core.plan.actions.tripActionsHeader
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.MockTripTemplates
import net.chronoflow.core.testing.compose.ProfileHost

class ActionsCase(
    val state: String,
    val profile: DeviceProfile,
    val language: AppLanguage,
    val dark: Boolean,
) {
    override fun toString() = "$state-${profile.name}-${language.tag}-$dark"
}

private val table by lazy { PageCopyTable.load() }

fun actionsUi(
    state: String = "online",
    language: Language = Language.En,
): TripActionsUi {
    val template = MockTripTemplates.templates().first()
    val detail =
        if (state ==
            "noPlan"
        ) {
            template.detail.copy(plan = template.detail.plan.copy(days = emptyList()))
        } else {
            template.detail
        }
    return TripActionsUi(
        template.card.id,
        tripActionsHeader(template.card, detail, PageCopy(language, table)),
        TripActionsAvailability(true, true, state != "offline", state == "busy"),
    )
}

fun actionsCases(): List<Array<Any>> =
    listOf("online", "noPlan", "offline").flatMap { state ->
        listOf(
            DeviceProfiles.small,
            DeviceProfiles.pixel8,
            DeviceProfiles.foldInner,
            DeviceProfiles.landscapeSideNav,
            DeviceProfiles.largeFont,
        ).flatMap { profile ->
            listOf(AppLanguage.En, AppLanguage.Nl).flatMap { language ->
                listOf(false, true).map { dark -> arrayOf<Any>(ActionsCase(state, profile, language, dark)) }
            }
        }
    }

@Composable
fun ActionsFixture(
    case: ActionsCase,
    modifier: Modifier = Modifier,
) {
    val ui = actionsUi(case.state, if (case.language == AppLanguage.Nl) Language.Nl else Language.En)
    ProfileHost(case.profile) {
        ChronoFlowTheme(darkTheme = case.dark) {
            Box(
                modifier
                    .fillMaxSize()
                    .background(CfTheme.colors.background)
                    .testTag("actions.fixture")
                    .windowInsetsPadding(LocalSafeInsets.current),
                contentAlignment = Alignment.BottomCenter,
            ) {
                if (case.state in setOf("busy", "failure")) {
                    TripActionsWindow(ui, TripActionFailure.Duplicate.takeIf { case.state == "failure" }, {}, {})
                } else {
                    Box(
                        Modifier.widthIn(max = 640.dp).background(CfTheme.colors.card),
                    ) { TripActionsContent(ui, {}, {}) }
                }
            }
        }
    }
}
