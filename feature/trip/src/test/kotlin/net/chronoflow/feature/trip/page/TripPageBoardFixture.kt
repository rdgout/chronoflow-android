
package net.chronoflow.feature.trip.page

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import net.chronoflow.core.designsystem.icon.MaterialSymbols
import net.chronoflow.core.designsystem.layout.LocalSafeInsets
import net.chronoflow.core.designsystem.system.CfNavItem
import net.chronoflow.core.designsystem.system.CfNavLayout
import net.chronoflow.core.designsystem.system.CfNavigationSuiteScaffold
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.model.contracts.EndState
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.NoteRole
import net.chronoflow.core.plan.generated.PlanMetrics
import net.chronoflow.core.plan.planDate
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.LargeTimelineFixtures
import net.chronoflow.core.testing.compose.BoardCaptureCell
import net.chronoflow.core.ui.R
import net.chronoflow.feature.trip.timeline.TextPaintMetrics
import net.chronoflow.feature.trip.timeline.TimelineTestFonts

class TripPageBoardCase(
    val board: String,
    val profile: DeviceProfile,
    val language: AppLanguage,
    val dark: Boolean = false,
) {
    val theme get() = if (dark) "dark" else "light"

    override fun toString() = "$board-${profile.name}-${language.tag}-$theme"
}

class TripPageBoardFixture(
    val case: TripPageBoardCase,
) {
    private val detail = LargeTimelineFixtures.fixture("S1").detail
    val dayIndex = if (case.board == "PlanTimelineTop") 0 else 1
    val rig =
        TripPageTestRig(
            "S1",
            checkNotNull(planDate(detail.plan.days[dayIndex].startUtc)).plusSeconds(9 * 3600L),
            startAtSummary = case.board == "PlanTimelineTop",
            initialTimeline = case.board != "PlanEnd" && case.board != "PlanEndNoText",
            language =
                if (case.language ==
                    AppLanguage.Nl
                ) {
                    Language.Nl
                } else {
                    Language.En
                },
            metrics = { TextPaintMetrics(TimelineTestFonts.typefaces(ApplicationProvider.getApplicationContext())) },
        )
    val blockTags get() =
        rig.model.prepared.days
            .flatMap { day -> day.blocks.map { "trip.block.${it.id}" } }
            .toSet()

    init {
        if (case.board == "PlanEndNoText") {
            val plan = detail.plan
            rig.replace(
                detail.copy(
                    plan =
                        plan.copy(
                            days =
                                plan.days.map { day ->
                                    day.copy(notes = day.notes.filterNot { it.role == NoteRole.End })
                                },
                            summary =
                                plan.summary?.copy(
                                    endState = EndState.Unknown("fixture-no-ending"),
                                    endCopy = emptyList(),
                                    expectedAdaptedText = null,
                                ),
                        ),
                ),
            )
        }
    }

    fun start(
        compose: ComposeContentTestRule,
        navigation: Boolean = false,
    ) {
        TimelineTestFonts.typefaces(ApplicationProvider.getApplicationContext())
        compose.setContent {
            ProfileHostContent(navigation)
        }
        compose.waitForIdle()
        position(compose)
    }

    @androidx.compose.runtime.Composable
    private fun ProfileHostContent(navigation: Boolean) {
        net.chronoflow.core.testing.compose.ProfileHost(case.profile) {
            ChronoFlowTheme(darkTheme = case.dark, reduceMotion = false) {
                Box(Modifier.fillMaxSize().background(CfTheme.colors.background).testTag(ROOT_TAG)) {
                    val safe = LocalSafeInsets.current
                    if (navigation) {
                        val items =
                            listOf(
                                CfNavItem(
                                    stringResource(R.string.screens_today),
                                    MaterialSymbols.SpaceDashboard,
                                    MaterialSymbols.SpaceDashboardFill,
                                    "tab.today",
                                ),
                                CfNavItem(
                                    stringResource(R.string.screens_my_trips),
                                    MaterialSymbols.Flight,
                                    MaterialSymbols.FlightFill,
                                    "tab.trips",
                                ),
                                CfNavItem(
                                    stringResource(R.string.screens_settings),
                                    MaterialSymbols.Settings,
                                    MaterialSymbols.SettingsFill,
                                    "tab.settings",
                                ),
                            )
                        CfNavigationSuiteScaffold(
                            if (case.profile.widthDp >=
                                600
                            ) {
                                CfNavLayout.Rail
                            } else {
                                CfNavLayout.Bar
                            },
                            items,
                            1,
                            {
                            },
                            Modifier.windowInsetsPadding(safe.only(WindowInsetsSides.Bottom)),
                        ) {
                            CompositionLocalProvider(
                                LocalSafeInsets provides
                                    safe.only(
                                        WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                                    ),
                            ) {
                                rig.Content()
                            }
                        }
                    } else {
                        Box(Modifier.windowInsetsPadding(safe.only(WindowInsetsSides.Bottom))) {
                            CompositionLocalProvider(
                                LocalSafeInsets provides
                                    safe.only(
                                        WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                                    ),
                            ) { rig.Content() }
                        }
                    }
                }
            }
        }
    }

    fun position(compose: ComposeContentTestRule) {
        val list = checkNotNull(rig.listState)
        val day = rig.model.prepared.days[dayIndex]
        when (case.board) {
            "PlanScrolled", "PlanDayMenu" -> {
                val offset = with(compose.density) { (9 * PlanMetrics.pointsPerHour).dp.roundToPx() }
                compose.runOnIdle { list.requestScrollToItem(rig.model.index.timelineIndex(day.id), offset) }
            }

            "PlanEnd", "PlanEndNoText" -> {
                val end =
                    if (case.profile.widthDp >= 600) rig.model.index.end else rig.model.listIndex.end
                compose.runOnIdle { list.requestScrollToItem(end) }
            }
        }
        compose.waitForIdle()
    }

    fun openMenu(compose: ComposeContentTestRule) {
        compose.onNodeWithTag("trip.dayMenu").performClick()
        compose.waitForIdle()
    }

    fun bitmap(compose: ComposeContentTestRule): Bitmap =
        tripBoardBitmap(compose, ROOT_TAG, case.board == "PlanDayMenu")

    fun close() {
        rig.close()
    }

    companion object {
        const val ROOT_TAG = "trip.board.root"
        val boards = listOf("Plan", "PlanScrolled", "PlanTimelineTop", "PlanDayMenu", "PlanEnd")

        fun guardCases(): List<TripPageBoardCase> {
            val profiles =
                listOf(
                    DeviceProfiles.small,
                    DeviceProfiles.pixel8,
                    DeviceProfiles.foldInner,
                    DeviceProfiles.landscapeSideNav,
                    DeviceProfiles.largeFont,
                )
            val primary =
                boards.flatMap { board ->
                    profiles.flatMap { profile ->
                        AppLanguage.entries.map { TripPageBoardCase(board, profile, it) }
                    }
                }
            val dark =
                listOf("Plan", "PlanScrolled").flatMap { board ->
                    listOf(DeviceProfiles.pixel8, DeviceProfiles.small).flatMap { profile ->
                        AppLanguage.entries.map { TripPageBoardCase(board, profile, it, true) }
                    }
                }
            val noText =
                listOf(DeviceProfiles.pixel8, DeviceProfiles.small).flatMap { profile ->
                    AppLanguage.entries.map { TripPageBoardCase("PlanEndNoText", profile, it) }
                }
            val minimum =
                AppLanguage.entries.map {
                    TripPageBoardCase(
                        "PlanTimelineTop",
                        DeviceProfiles.small.copy(name = "minimum280", widthDp = 280),
                        it,
                    )
                }
            return primary + dark + noText + minimum
        }

        fun captureCases(): List<TripPageBoardCase> =
            (boards + "PlanEndNoText").flatMap { board ->
                val cells =
                    if (board in setOf("Plan", "PlanScrolled", "PlanDayMenu")) {
                        BoardCaptureCell.phoneWithWideSibling
                    } else {
                        BoardCaptureCell.standard
                    }
                cells.map { cell -> TripPageBoardCase(board, cell.profile, cell.language, cell.dark) }
            }
    }
}

fun tripBoardBitmap(
    compose: ComposeContentTestRule,
    rootTag: String,
    menu: Boolean,
): Bitmap {
    val root = compose.onNodeWithTag(rootTag)
    val bitmap = checkNotNull(root.captureToImage().asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, true))
    if (menu) {
        val popup = compose.onNode(isPopup())
        val offset = popup.fetchSemanticsNode().positionOnScreen - root.fetchSemanticsNode().positionOnScreen
        val menuBitmap = popup.captureToImage().asAndroidBitmap()
        check(offset.x.isFinite() && offset.y.isFinite())
        check(offset.x >= -1 && offset.y >= -1)
        check(offset.x + menuBitmap.width <= bitmap.width + 1 && offset.y + menuBitmap.height <= bitmap.height + 1)
        Canvas(bitmap).drawBitmap(menuBitmap, offset.x, offset.y, null)
    }
    return bitmap
}
