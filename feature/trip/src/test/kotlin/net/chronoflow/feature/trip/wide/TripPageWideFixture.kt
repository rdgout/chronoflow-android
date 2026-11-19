package net.chronoflow.feature.trip.wide

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.plan.planDate
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.LargeTimelineFixtures
import net.chronoflow.core.testing.SafeInsetsDp
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.feature.trip.page.TripPageTestRig
import net.chronoflow.feature.trip.state.TripStatesFixture
import net.chronoflow.feature.trip.step.TripPageSheets
import net.chronoflow.feature.trip.timeline.TextPaintMetrics
import net.chronoflow.feature.trip.timeline.TimelineTestFonts

class TripPageWideCase(
    val board: String,
    val profile: DeviceProfile = board669,
    val language: AppLanguage = AppLanguage.En,
    val dark: Boolean = false,
    val position: String = "day",
) {
    val name get() = "${board}_${profile.name}_${language.tag}_${if (dark) "dark" else "light"}"

    override fun toString() = "$name-$position"

    companion object {
        val board669 =
            DeviceProfiles.foldInner.copy(
                name = "board669",
                widthDp = 669,
                heightDp = 951,
                insets = SafeInsetsDp(0f, 0f, 0f, 0f),
            )

        fun captures(): List<TripPageWideCase> =
            listOf(
                TripPageWideCase("PlanWide", position = "top"),
                TripPageWideCase("PlanWideDark", dark = true),
                TripPageWideCase("PlanWideScrolled", position = "scrolled"),
                TripPageWideCase("PlanWideDayMenu_title", position = "scrolled"),
            ) +
                listOf("PlanRemoteWide", "PlanRemoteWideOffline").flatMap { board ->
                    AppLanguage.entries.flatMap { language ->
                        listOf(false, true).map { dark ->
                            TripPageWideCase(board, language = language, dark = dark)
                        }
                    }
                } +
                listOf(
                    TripPageWideCase(
                        "PlanWide",
                        board669.copy(name = "board669-xxl", fontScale = 2f),
                        AppLanguage.Nl,
                        position = "top",
                    ),
                    TripPageWideCase(
                        "PlanRemoteWide",
                        board669.copy(name = "board669-xxl", fontScale = 2f),
                        AppLanguage.Nl,
                        true,
                    ),
                    TripPageWideCase(
                        "PlanRemoteWideOffline",
                        board669.copy(name = "board669-xxl", fontScale = 2f),
                        AppLanguage.Nl,
                        true,
                    ),
                    TripPageWideCase("PlanWide", DeviceProfiles.foldInner),
                    TripPageWideCase("PlanWide", DeviceProfiles.landscapeSideNav),
                )

        fun guards(): List<TripPageWideCase> =
            listOf("ready-top", "ready-scrolled", "PlanRemoteWide", "PlanRemoteWideOffline").flatMap { state ->
                AppLanguage.entries.flatMap { language ->
                    listOf(false, true).map { dark ->
                        TripPageWideCase(
                            state,
                            DeviceProfiles.foldInner,
                            language,
                            dark,
                            if (state == "ready-top") {
                                "top"
                            } else if (state == "ready-scrolled") {
                                "scrolled"
                            } else {
                                "day"
                            },
                        )
                    }
                }
            } +
                listOf("PlanWide", "PlanRemoteWideOffline").flatMap { state ->
                    listOf(
                        TripPageWideCase(state, DeviceProfiles.landscapeSideNav),
                        TripPageWideCase(state, DeviceProfiles.landscapeSideNav, AppLanguage.Nl, true),
                    )
                } +
                listOf("ready-scrolled", "PlanRemoteWideOffline").map { state ->
                    TripPageWideCase(
                        state,
                        DeviceProfiles.foldInner.copy(name = "foldInner-xxl", fontScale = 2f),
                        AppLanguage.Nl,
                        true,
                        if (state == "ready-scrolled") "scrolled" else "day",
                    )
                } +
                listOf("step", "legend", "PlanNoPlan", "PlanLoading", "ErrorNotFound", "ErrorServer").map {
                    TripPageWideCase(it, DeviceProfiles.foldInner)
                } + TripPageWideCase("PlanWide", board669)
    }
}

class TripPageWideFixture(
    val case: TripPageWideCase,
) {
    private val stateBoard =
        when (case.board) {
            "PlanRemoteWide" -> "PlanRemote"
            "PlanRemoteWideOffline" -> "PlanRemoteOffline"
            "PlanNoPlan", "PlanLoading", "ErrorNotFound", "ErrorServer" -> case.board
            else -> null
        }
    private val remote = stateBoard?.let { TripStatesFixture(it, case.profile, case.language, case.dark) }
    private val fixture = LargeTimelineFixtures.fixture("S1").detail
    val rig =
        remote?.rig ?: TripPageTestRig(
            "S1",
            clockStart = checkNotNull(planDate(fixture.plan.days[1].startUtc)).plusSeconds(9 * 3600),
            startAtSummary = case.position == "top",
            language = if (case.language == AppLanguage.Nl) Language.Nl else Language.En,
            metrics = { TextPaintMetrics(TimelineTestFonts.typefaces(ApplicationProvider.getApplicationContext())) },
        )
    val day get() = rig.model.prepared.days[1]
    val blockTags get() =
        rig.model.prepared.days
            .flatMap {
                it.blocks.map { block ->
                    "trip.block.${block.id}"
                }
            }.toSet()

    fun start(compose: ComposeContentTestRule) {
        TimelineTestFonts.typefaces(ApplicationProvider.getApplicationContext())
        compose.setContent { Content() }
        compose.waitForIdle()
        if (remote == null && case.position == "scrolled") {
            compose.runOnIdle {
                rig.listState!!.requestScrollToItem(
                    rig.model.index.timelineIndex(day.id),
                    with(compose.density) { 360.dp.roundToPx() },
                )
            }
            compose.waitForIdle()
        }
        if (case.board == "step") {
            compose.runOnIdle { rig.vm.openStep(day.blocks.first().id) }
            compose.waitForIdle()
        } else if (case.board == "legend") {
            compose.runOnIdle { rig.vm.openLegend() }
            compose.waitForIdle()
        }
    }

    @Composable private fun Content() {
        ProfileHost(case.profile) {
            ChronoFlowTheme(darkTheme = case.dark, reduceMotion = true) {
                Box(Modifier.fillMaxSize().background(CfTheme.colors.background).testTag(ROOT)) {
                    rig.Content()
                    TripPageSheets(rig.vm)
                }
            }
        }
    }

    fun close() {
        rig.close()
    }

    companion object {
        const val ROOT = "trip.wide.board"
    }
}
