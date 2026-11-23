@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.today

import androidx.activity.FullyDrawnReporter
import androidx.activity.FullyDrawnReporterOwner
import androidx.activity.compose.LocalFullyDrawnReporterOwner
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.FakePlanClock
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.tiers.Logic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.Executor

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h914dp-420dpi")
class TodayFullyDrawnTest {
    @get:Rule val compose = createComposeRule()
    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val source = FakeTodaySource().apply { synced.value = false }
    private val model =
        TodayViewModel(source, FakePlanClock(todayNow), { todayFormatter(dispatcher) }, dispatcher, dispatcher)
    private val store = ViewModelStore().apply { put("today", model) }
    private var reports = 0
    private val owner =
        object : FullyDrawnReporterOwner {
            override val fullyDrawnReporter = FullyDrawnReporter(Executor { it.run() }) { reports++ }
        }

    @After fun close() {
        store.clear()
        scheduler.runCurrent()
    }

    private fun host() {
        owner.fullyDrawnReporter.addReporter()
        compose.setContent {
            CompositionLocalProvider(LocalFullyDrawnReporterOwner provides owner) {
                ProfileHost(DeviceProfiles.pixel8) {
                    ChronoFlowTheme(reduceMotion = true) { TodayRoute(model, noTodayActions) }
                }
            }
        }
        scheduler.runCurrent()
        compose.waitForIdle()
    }

    private fun contentReady() {
        source.synced.value = true
        compose.waitUntil(5_000) {
            scheduler.runCurrent()
            model.uiState.value !is TodayUiState.Loading
        }
        compose.waitForIdle()
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
    }

    @Test fun loadingHoldsTheReportAfterTheOverlayEnds() {
        host()
        compose.runOnIdle { owner.fullyDrawnReporter.removeReporter() }
        assertEquals(0, reports)
        contentReady()
        assertEquals(1, reports)
        compose.mainClock.advanceTimeByFrame()
        assertEquals(1, reports)
    }

    @Test fun contentStillWaitsForTheOverlayReporter() {
        host()
        contentReady()
        assertEquals(0, reports)
        compose.runOnIdle { owner.fullyDrawnReporter.removeReporter() }
        compose.waitForIdle()
        assertEquals(1, reports)
    }
}
