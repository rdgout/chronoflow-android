@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.app.shell

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import net.chronoflow.core.datamock.MockAppSeed
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.TripCardView
import net.chronoflow.core.model.contracts.TripDetail
import net.chronoflow.core.model.store.TripRecord
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.navigation.shell.ShellNav
import net.chronoflow.core.navigation.shell.ShellTab
import net.chronoflow.core.plan.CopyCatalog
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.plan.trippage.PlanPreferences
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.FakePlanClock
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.today.TodayRoute
import net.chronoflow.feature.today.TodayViewModel
import net.chronoflow.feature.today.data.TodaySource
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class, qualifiers = "w411dp-h914dp-420dpi")
class TodayNavigationTest {
    @get:Rule val compose = createComposeRule()
    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val now = Instant.parse("2026-11-08T10:00:00Z")
    private val source = FakeTodaySource(now)
    private val model =
        TodayViewModel(
            source,
            FakePlanClock(now),
            { CopyLineFormatter(CopyCatalog.load(dispatcher)) },
            dispatcher,
            dispatcher,
        )
    private val store = ViewModelStore().apply { put("today", model) }
    private val holder = ShellViewModel(ShellNav.initial()).also { store.put("shell", it) }
    private lateinit var activity: ComponentActivity

    @After fun close() {
        store.clear()
        scheduler.runCurrent()
    }

    private fun host() {
        compose.setContent {
            ProfileHost(DeviceProfiles.pixel8) {
                ChronoFlowTheme(reduceMotion = true) {
                    val currentActivity = LocalActivity.current as ComponentActivity
                    SideEffect { activity = currentActivity }
                    AppShellScaffold(
                        holder,
                        {},
                        today = { TodayRoute(model, todayActions(holder) {}) },
                        trips = { Box(Modifier.fillMaxSize().testTag("trips.root")) },
                        settings = { Box(Modifier.fillMaxSize().testTag("settings.root")) },
                        page = { Box(Modifier.fillMaxSize().testTag("trip.page")) },
                        overlay = { Box(Modifier.fillMaxSize().testTag("wizard.root")) },
                    )
                }
            }
        }
        waitFor { compose.onNodeWithTag("today.screen").isDisplayed() }
    }

    private fun waitFor(condition: () -> Boolean) {
        compose.waitUntil(5_000) {
            scheduler.runCurrent()
            condition()
        }
        compose.waitForIdle()
    }

    @Test fun heroPushesOnTodayAndSystemBackReturns() {
        host()
        waitFor { compose.onNodeWithTag("today.trip").isDisplayed() }
        compose.onNodeWithTag("today.trip").performClick()
        assertEquals(ShellTab.Today, holder.state.selected)
        assertEquals(
            Route.TripPage(source.heroId),
            holder.state.stacks
                .getValue(ShellTab.Today)
                .last(),
        )
        compose.onNodeWithTag("trip.page").assertExists()
        compose.runOnIdle { activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("today.screen").assertExists()
        assertEquals(
            Route.Today,
            holder.state.stacks
                .getValue(ShellTab.Today)
                .single(),
        )
    }
}

private class FakeTodaySource(
    now: Instant,
) : TodaySource {
    private val account =
        MockAppSeed
            .make(now, walkthroughSeen = true)
            .accounts.values
            .minBy { it.snapshot.id }
    val heroId =
        account.trips
            .first {
                it.card.progress.status == net.chronoflow.core.model.contracts.TripStatus.Active
            }.card.id
    val preferences = MutableStateFlow(PlanPreferences())
    val records =
        MutableStateFlow(
            account.trips.map { item ->
                TripRecord(
                    item.card.id,
                    ApiJson.response.encodeToString(TripCardView.serializer(), item.card),
                    currentInputHash =
                        account.details[item.card.id]
                            ?.plan
                            ?.meta
                            ?.inputHash,
                    departureUtc = item.card.departureUtc,
                    planStartUtc = item.planStartUtc,
                    planEndUtc = item.planEndUtc,
                    planStartZone = item.planStartZone,
                    planEndDate = item.planEndDate,
                    preparationDays = item.preparationDays,
                )
            },
        )

    override fun synced() = MutableStateFlow(true)

    override fun preferences() = preferences

    override fun trips() = records

    override fun detail(tripId: String) = MutableStateFlow(account.details[tripId])

    override suspend fun setCheck(
        tripId: String,
        detail: TripDetail,
        blockId: String,
        status: CheckStatus?,
    ) = Unit
}
