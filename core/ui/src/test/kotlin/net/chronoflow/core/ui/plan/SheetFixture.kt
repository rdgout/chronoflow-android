package net.chronoflow.core.ui.plan

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
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.core.plan.CopyCatalog
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.plan.planDate
import net.chronoflow.core.plan.step.StepActions
import net.chronoflow.core.plan.step.StepBadge
import net.chronoflow.core.plan.step.StepSheetState
import net.chronoflow.core.plan.step.stepSheetState
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.LargeTimelineFixtures
import net.chronoflow.core.testing.compose.ProfileHost

class SheetCase(
    val state: String,
    val profile: DeviceProfile,
    val language: AppLanguage,
    val dark: Boolean,
) {
    override fun toString() = "$state-${profile.name}-${language.tag}-${if (dark) "dark" else "light"}"
}

fun sheetCases(states: List<String>): List<Array<Any>> =
    states.flatMap { state ->
        listOf(
            DeviceProfiles.small,
            DeviceProfiles.pixel8,
            DeviceProfiles.foldInner,
            DeviceProfiles.landscapeSideNav,
            DeviceProfiles.largeFont,
        ).flatMap { profile ->
            listOf(AppLanguage.En, AppLanguage.Nl).flatMap { language ->
                listOf(false, true).map { dark -> arrayOf<Any>(SheetCase(state, profile, language, dark)) }
            }
        }
    }

private val sampleDetail by lazy { LargeTimelineFixtures.fixture("S1").detail }
private val sampleFormatter by lazy {
    val bytes =
        checkNotNull(CopyCatalog::class.java.getResourceAsStream("/plan_copy.json"))
            .use { it.readBytes() }
    CopyLineFormatter(CopyCatalog(bytes))
}

fun sheetState(
    state: String = "running",
    dutch: Boolean = false,
): StepSheetState {
    if (state == "gone") return StepSheetState.Gone
    val block = sampleDetail.plan.blocks.first { it.type == BlockType.AvoidLight }
    val start = checkNotNull(planDate(block.startUtc)).toEpochMilli()
    val result =
        stepSheetState(
            sampleDetail.copy(checkState = sampleDetail.checkState.copy(marks = emptyList())),
            block.id,
            if (state == "running") start else start - 60_000,
            sampleFormatter,
            if (dutch) Language.Nl else Language.En,
            TimeFormat.Value24,
        ) as StepSheetState.Step
    return when (state) {
        "done" -> result.copy(badge = StepBadge.Done, actions = StepActions.Marked)
        "skipped" -> result.copy(badge = StepBadge.Skipped, actions = StepActions.Marked)
        "information" -> result.copy(priorityKey = "Information", actions = StepActions.None)
        else -> result
    }
}

@Composable
fun SheetFixture(
    case: SheetCase,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    ProfileHost(case.profile) {
        ChronoFlowTheme(darkTheme = case.dark) {
            Box(
                modifier
                    .fillMaxSize()
                    .background(CfTheme.colors.background)
                    .testTag("sheet.fixture")
                    .windowInsetsPadding(LocalSafeInsets.current),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(Modifier.widthIn(max = 640.dp).background(CfTheme.colors.card)) { content() }
            }
        }
    }
}
