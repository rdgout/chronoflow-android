package net.chronoflow.feature.trip.state

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import net.chronoflow.core.designsystem.icon.MaterialSymbols
import net.chronoflow.core.designsystem.layout.LocalSafeInsets
import net.chronoflow.core.designsystem.system.CfNavItem
import net.chronoflow.core.designsystem.system.CfNavLayout
import net.chronoflow.core.designsystem.system.CfNavigationSuiteScaffold
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.model.contracts.AdaptationView
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.PlanStatus
import net.chronoflow.core.model.contracts.TripCardView
import net.chronoflow.core.model.contracts.TripDetail
import net.chronoflow.core.model.store.PlanVersionRecord
import net.chronoflow.core.model.store.TripPlanSnapshot
import net.chronoflow.core.model.store.TripRecord
import net.chronoflow.core.plan.planDate
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.LargeTimelineFixtures
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.ui.R
import net.chronoflow.feature.trip.page.TripPageEvent
import net.chronoflow.feature.trip.page.TripPageTestRig
import net.chronoflow.feature.trip.timeline.TimelineTestFonts

class TripStatesFixture(
    val board: String,
    val profile: DeviceProfile = DeviceProfiles.pixel8,
    val language: AppLanguage = AppLanguage.En,
    val dark: Boolean = false,
) {
    private val fixtureName =
        when (board) {
            "PlanNoPlan" -> "S3"

            "PlanRemote", "PlanRemoteLoadingNL", "PlanRemoteOffline", "PlanRemoteOfflineNL",
            "ErrorNotFound", "ErrorServer",
            -> "S2"

            else -> "S1"
        }
    private val template = LargeTimelineFixtures.fixture(fixtureName)
    private val detail =
        if (board == "PlanNoPlan") {
            template.detail.copy(plan = template.detail.plan.copy(status = PlanStatus.NoPlan))
        } else {
            template.detail
        }
    private val ready = board in setOf("PlanList", "PlanListScrolled", "Ready", "PlanNoPlan", "Tombstone")
    val path =
        FakePath().also {
            if (board in setOf("PlanRemoteOffline", "PlanRemoteOfflineNL")) it.values.trySend(false)
        }
    var failure: DataFailure? =
        when (board) {
            "ErrorNotFound" -> DataFailure.Server(404)
            "ErrorServer" -> DataFailure.Server(500)
            else -> null
        }
    private val requestGate = CompletableDeferred<Unit>()
    var requests = 0
        private set
    val events = mutableListOf<TripPageEvent>()
    val rig =
        TripPageTestRig(
            fixture = fixtureName,
            clockStart = checkNotNull(planDate(detail.plan.days[0].startUtc)).plusSeconds(9 * 3600),
            startAtSummary = board == "PlanList",
            language = if (language == AppLanguage.Nl) Language.Nl else Language.En,
            initialTimeline = board != "PlanList" && board != "PlanListScrolled",
            initialSnapshot = snapshot(),
            fetchDetail = {
                requests++
                failure?.let { throw it }
                requestGate.await()
            },
            makePath = { path },
        )

    private fun snapshot(): TripPlanSnapshot {
        if (board == "Missing") return TripPlanSnapshot(plan = null)
        val card = template.card
        val adaptation =
            detail.plan.summary?.adaptation?.let {
                ApiJson.response.encodeToString(AdaptationView.serializer(), it)
            }
        val record =
            TripRecord(
                id = card.id,
                card = ApiJson.response.encodeToString(TripCardView.serializer(), card),
                adaptation = adaptation,
                currentInputHash = detail.plan.meta.inputHash,
                keepOffline = board == "PlanLoading" || ready,
                departureUtc = card.departureUtc,
                planEndDate =
                    detail.plan.days
                        .last()
                        .labelDate,
                deletedLocally = board == "Tombstone",
            )
        val version =
            if (ready) {
                PlanVersionRecord(
                    card.id,
                    detail.plan.meta.inputHash,
                    ApiJson.response.encodeToString(TripDetail.serializer(), detail).encodeToByteArray(),
                    "2026-01-01T00:00:00Z",
                )
            } else {
                null
            }
        return TripPlanSnapshot(record, version)
    }

    fun start(
        compose: ComposeContentTestRule,
        navigation: Boolean = false,
    ) {
        TimelineTestFonts.typefaces(ApplicationProvider.getApplicationContext())
        compose.setContent { Content(navigation = navigation) }
        compose.waitForIdle()
        if (board == "PlanListScrolled") {
            val row = rig.model.listRows[0].rows[0]
            compose.runOnIdle { rig.listState!!.requestScrollToItem(rig.model.listIndex.rowIndex(row.key), 20) }
            compose.waitForIdle()
        }
    }

    @Composable fun Content(
        modifier: Modifier = Modifier,
        navigation: Boolean = false,
    ) {
        ProfileHost(profile) {
            ChronoFlowTheme(darkTheme = dark, reduceMotion = true) {
                val safe = LocalSafeInsets.current
                val contentSafe = safe.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                Box(modifier.fillMaxSize().background(CfTheme.colors.background).testTag(ROOT_TAG)) {
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
                            if (profile.widthDp >= 600) CfNavLayout.Rail else CfNavLayout.Bar,
                            items,
                            1,
                            {},
                            Modifier.windowInsetsPadding(safe.only(WindowInsetsSides.Bottom)),
                        ) {
                            CompositionLocalProvider(LocalSafeInsets provides contentSafe) { Page() }
                        }
                    } else {
                        Box(Modifier.windowInsetsPadding(safe.only(WindowInsetsSides.Bottom))) {
                            CompositionLocalProvider(LocalSafeInsets provides contentSafe) { Page() }
                        }
                    }
                }
            }
        }
    }

    @Composable private fun Page() {
        val scope = rememberCoroutineScope()
        val handler: (TripPageEvent) -> Unit =
            remember(rig, scope) {
                { event ->
                    events += event
                    when (event) {
                        TripPageEvent.Retry -> rig.vm.retry()
                        TripPageEvent.ToggleView -> rig.vm.toggleView()
                        TripPageEvent.Refresh -> scope.launch { rig.vm.refresh() }
                        is TripPageEvent.Select -> rig.vm.select(event.dayId, event.offsetDp)
                        TripPageEvent.SaveAnchor -> rig.vm.saveAnchor()
                        is TripPageEvent.Jump -> rig.vm.jump(event.dayId)
                        else -> Unit
                    }
                }
            }
        rig.Content(onEvent = handler, canToday = true)
    }

    fun close() {
        rig.close()
    }

    companion object {
        const val ROOT_TAG = "trip.states.root"
    }
}
