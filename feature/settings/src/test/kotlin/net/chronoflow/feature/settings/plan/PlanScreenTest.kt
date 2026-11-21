@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.settings.plan

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.settings.support.FakeSettingsDependencies
import net.chronoflow.feature.settings.support.SettingsScreenTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PlanScreenTest : SettingsScreenTest() {
    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val store = ViewModelStore()
    private var model: PlanViewModel? = null

    private fun state(
        credits: String? = """{"credits":2}""",
        subscription: String = "{}",
        loaded: Boolean = true,
        offline: Boolean = false,
    ): PlanScreenState =
        PlanScreenState(
            SettingsPlanState(
                credits?.let { ApiJson.response.parseToJsonElement(it) as JsonObject },
                ApiJson.response.parseToJsonElement("""{"subscription":$subscription}""") as JsonObject,
                loaded,
                offline,
            ),
            SettingsPlanLine("Renews on {{date}} · {{interval}}", "14 Nov 2026", "yearly"),
        )

    @After fun clearModel() =
        runTest(dispatcher, timeout = 60.seconds) {
            withContext(NonCancellable) {
                store.clear()
                model
                    ?.viewModelScope
                    ?.coroutineContext
                    ?.job
                    ?.cancelAndJoin()
            }
        }

    @Test fun loadingIsOneNodeWithTheLoadingDescription() {
        show { PlanContent(state(loaded = false), {}) }
        compose.onAllNodesWithTag("settings.plan.loading").assertCountEquals(1)
        compose.onNodeWithTag("settings.plan.hero").assertDoesNotExist()
    }

    @Test fun creditsShowHeroAndNoLink() {
        show { PlanContent(state(), {}) }
        compose.onNodeWithTag("settings.plan.hero").assertExists()
        compose.onNodeWithTag("settings.plan.noLink").assertExists()
    }

    @Test fun unlimitedShowsRenewLineAndNoNoLink() {
        show { PlanContent(state("""{"unlimited":true}"""), {}) }
        compose.onNodeWithTag("settings.plan.date").assertExists()
        compose.onNodeWithTag("settings.plan.noLink").assertDoesNotExist()
    }

    @Test fun cancelledShowsBadgeAndEndsLine() {
        val state =
            state("""{"unlimited":true}""", """{"canResume":true}""")
                .copy(
                    line =
                        SettingsPlanLine(
                            "Ends on {{date}}. After that, you can buy credits per trip.",
                            "14 Nov 2026",
                            "yearly",
                        ),
                )
        show { PlanContent(state, {}) }
        compose.onNodeWithTag("settings.plan.cancelled").assertExists()
        compose.onNodeWithTag("settings.plan.date").assertExists()
    }

    @Test fun failedShowsTheWarningNotice() {
        show { PlanContent(state(subscription = """{"paymentFailed":true}"""), {}) }
        compose.onNodeWithTag("settings.plan.failed").assertExists()
    }

    @Test fun missingBalanceShowsStoredStateAndRetryRefreshes() {
        var refreshed = false
        show { PlanContent(state(credits = null), { refreshed = true }) }
        compose.onNodeWithTag("settings.missing").assertExists()
        compose.onNodeWithTag("settings.stored.retry").performClick()
        assertTrue(refreshed)
    }

    @Test fun offlineShowsTheLine() {
        show { PlanContent(state(offline = true), {}) }
        compose.onNodeWithTag("settings.plan.offline").assertExists()
    }

    @Test fun pullCallsRefreshNowAndHidesTheIndicatorWhenItReturns() {
        val returned = CompletableDeferred<Unit>()
        var requested = false
        val deps =
            FakeSettingsDependencies().apply {
                refresh = {
                    requested = true
                    returned.await()
                }
            }
        val model = PlanViewModel(deps, dispatcher, dispatcher)
        this.model = model
        store.put("plan", model)
        scheduler.runCurrent()
        show { PlanRoute(model, {}) }
        scheduler.runCurrent()
        compose.onNodeWithTag("settings.plan.screen").performTouchInput {
            swipeDown(
                startY = height * 0.35f,
                endY =
                    height * 0.9f,
            )
        }
        compose.waitForIdle()
        scheduler.runCurrent()
        assertTrue(requested)
        assertTrue(model.ui.value.refreshing)
        returned.complete(Unit)
        scheduler.runCurrent()
        assertFalse(model.ui.value.refreshing)
    }
}
