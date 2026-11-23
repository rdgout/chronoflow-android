package net.chronoflow.app.reminders

import android.Manifest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.core.app.NotificationManagerCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.chronoflow.app.ChronoFlowApp
import net.chronoflow.app.ioViolationsForTest
import net.chronoflow.app.testing.ScreenTest
import net.chronoflow.core.data.reminders.AndroidReminderAlarms
import net.chronoflow.core.data.repository.TripRepository
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.TripCardView
import net.chronoflow.core.plan.PlanDecoder
import net.chronoflow.core.reminders.ReminderRequest
import net.chronoflow.core.testing.tiers.Smoke
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@Smoke
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class ReminderDeliveryTest : ScreenTest() {
    @get:Rule val notifications = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    @Test fun alarmPostsAndTapOpensItsTripStep() {
        val started = System.nanoTime()
        launch(
            mapOf(
                "CF_AUTH_TEST_SIGNED_IN" to "demo",
                "CF_AUTH_TEST_WALKTHROUGH_SEEN" to "1",
                "CF_MOCK_DELAY" to "0",
                "cf.settings.language" to "en",
                "cf.reduceMotion" to "1",
            ),
        )
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val app = context.applicationContext as ChronoFlowApp
        val device = UiDevice.getInstance(instrumentation)
        compose.waitUntilExactlyOneExists(hasTestTag("tab.today"), 5_000)
        runBlocking(Dispatchers.IO) {
            val database = checkNotNull(app.graph.sessionAccounts.activeAccountDatabase())
            val repository = TripRepository(database)
            val trip =
                repository.trips().first { record ->
                    val card = ApiJson.response.decodeFromString(TripCardView.serializer(), record.card)
                    card.from.iata == "AMS" && card.to.iata == "HND"
                }
            val hash = checkNotNull(trip.currentInputHash)
            val detail = PlanDecoder(app.graph.dispatchers.default).detail(checkNotNull(repository.plan(trip.id, hash)))
            val block = detail.plan.blocks.first { it.type == BlockType.SeekLight }
            val now = Instant.now()
            AndroidReminderAlarms(context, Dispatchers.IO).set(
                ReminderRequest(
                    identifier = IDENTIFIER,
                    title = TITLE,
                    body = "Seek bright light from 01:00.",
                    sendAt = now.plusSeconds(2),
                    tripID = trip.id,
                    blockID = block.id,
                    inputHash = hash,
                    memberKeys = listOf("seek_light|x|1"),
                    accountID = checkNotNull(database.accountID).rawValue,
                    expiresAt = now.plusSeconds(600),
                ),
                REQUEST_CODE,
            )
        }
        device.openNotification()
        checkNotNull(device.wait(Until.findObject(By.text(TITLE)), 6_000)) { "Reminder did not appear" }.click()
        compose.waitUntilExactlyOneExists(hasTestTag("trip.page"), 5_000)
        compose.waitUntilExactlyOneExists(hasTestTag("step.sheet"), 5_000)
        instrumentation.runOnMainSync { assertEquals(0, app.graph.ioViolationsForTest) }
        assertTrue("reminder journey exceeded 10 seconds", System.nanoTime() - started < 10_000_000_000L)
    }

    @After fun cancelReminder() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        runBlocking(Dispatchers.IO) {
            AndroidReminderAlarms(context, Dispatchers.IO).cancel(IDENTIFIER, REQUEST_CODE)
            NotificationManagerCompat.from(context).cancelAll()
        }
    }

    private companion object {
        const val IDENTIFIER = "chronoflow.reminder.e2e"
        const val REQUEST_CODE = 4242
        const val TITLE = "Amsterdam → Tokyo"
    }
}
