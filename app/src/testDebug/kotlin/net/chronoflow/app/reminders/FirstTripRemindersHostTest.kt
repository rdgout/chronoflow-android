@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.app.reminders

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.reminders.ReminderPermission
import net.chronoflow.core.reminders.ReminderSettings
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.debug.reminders.MockReminderCenter
import net.chronoflow.feature.reminders.FirstTripRemindersOfferViewModel
import net.chronoflow.feature.reminders.PermissionRequestStart
import net.chronoflow.feature.reminders.RemindersDependencies
import net.chronoflow.feature.reminders.RemindersViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = android.app.Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FirstTripRemindersHostTest {
    private val compose = createEmptyComposeRule()

    @get:Rule val rules: RuleChain =
        RuleChain
            .outerRule(
                net.chronoflow.core.testing.compose
                    .ProfileRule(DeviceProfiles.pixel8),
            ).around(compose)

    @Test fun askPermissionAndFirstTripSeam() {
        val scheduler = TestCoroutineScheduler()
        val main = StandardTestDispatcher(scheduler)
        val deps = Dependencies()
        RemindersHostActivity.content = {
            ProfileHost(DeviceProfiles.pixel8) {
                ChronoFlowTheme(reduceMotion = true) { FirstTripRemindersHost(deps, main, "trip") }
            }
        }
        val controller = Robolectric.buildActivity(RemindersHostActivity::class.java).setup()
        var modelJob: Job? = null
        var offerJob: Job? = null
        try {
            flush(scheduler)
            modelJob =
                ViewModelProvider
                    .create(
                        controller.get(),
                        RemindersViewModel.Factory,
                    )["trip:trip:firstTrip", RemindersViewModel::class.java]
                    .viewModelScope.coroutineContext[Job]
            offerJob =
                ViewModelProvider
                    .create(
                        controller.get(),
                        FirstTripRemindersOfferViewModel.Factory,
                    )["trip:trip:remindersOffer", FirstTripRemindersOfferViewModel::class.java]
                    .viewModelScope.coroutineContext[Job]
            compose.onNodeWithTag("reminders.firstTrip.enable").assertExists()
            assertFalse(deps.pending)
            assertEquals(1, deps.consumes)
            compose.onNodeWithTag("reminders.firstTrip.enable").performClick()
            flush(scheduler)
            compose.onNodeWithTag("reminders.firstTrip").assertDoesNotExist()
            assertEquals(1, deps.center.permissionRequestCount)
            controller.recreate()
            flush(scheduler)
            compose.onNodeWithTag("reminders.firstTrip").assertDoesNotExist()
            assertEquals(1, deps.consumes)
        } finally {
            controller.pause().stop().destroy()
            try {
                runTest(main, timeout = 60.seconds) {
                    withContext(NonCancellable) {
                        modelJob?.cancelAndJoin()
                        offerJob?.cancelAndJoin()
                    }
                }
            } finally {
                RemindersHostActivity.content = null
            }
        }
    }

    private fun flush(scheduler: TestCoroutineScheduler) {
        compose.waitForIdle()
        scheduler.runCurrent()
        compose.waitForIdle()
        scheduler.runCurrent()
        compose.waitForIdle()
    }

    companion object {
        @JvmStatic @BeforeClass
        fun warmNativeRuntime() {
            net.chronoflow.core.testing.junit.measureClassWarmup(
                FirstTripRemindersHostTest::class.java,
                36,
                "native-font-runtime",
            ) {
                org.robolectric.nativeruntime.DefaultNativeRuntimeLoader
                    .injectAndLoad()
                android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
            }
        }
    }

    private class Dependencies : RemindersDependencies {
        val center = MockReminderCenter(ReminderPermission.Undetermined)
        var pending = true
        var consumes = 0

        override fun reminderSettings() = MutableStateFlow<ReminderSettings?>(ReminderSettings())

        override fun settingsChanges() = MutableStateFlow(Unit)

        override suspend fun permission() = center.permission()

        override suspend fun beginPermissionRequest(): PermissionRequestStart {
            center.requestPermission()
            return PermissionRequestStart.NoDialog
        }

        override suspend fun enqueueReminders(patch: JsonObject) { }

        override suspend fun reschedule() { }

        override suspend fun consumeFirstTripOffer(permission: ReminderPermission): Boolean {
            consumes++
            return pending.also {
                pending =
                    false
            }
        }

        override suspend fun isCurrent() = true
    }
}

class RemindersHostActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { content?.invoke() }
    }

    companion object {
        var content: (@Composable () -> Unit)? = null
    }
}
