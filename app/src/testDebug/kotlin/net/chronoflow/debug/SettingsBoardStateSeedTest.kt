package net.chronoflow.debug

import android.app.Application
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.datamock.FileMockStateStore
import net.chronoflow.core.datamock.MockAppSeed
import net.chronoflow.core.datamock.MockScenarioValues
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class SettingsBoardStateSeedTest(
    private val state: String,
    private val gated: Boolean,
) {
    @Test fun boardStateIsStoredOnlyWithAnAuthTestId() =
        runTest(timeout = 60.seconds) {
            val io = StandardTestDispatcher(testScheduler)
            val context = RuntimeEnvironment.getApplication() as Application
            val root = context.filesDir.resolve("board-$state-$gated")
            val now = Instant.parse("2026-11-21T12:00:00Z")
            val original = MockAppSeed.make(now, walkthroughSeen = true)
            val extras =
                buildMap {
                    put("CF_SETTINGS_BOARD_STATE", state)
                    put("CF_AUTH_TEST_WALKTHROUGH_SEEN", "1")
                    if (gated) put(TestSeed.TEST_ID_KEY, "019a562b-8000-7000-8000-000000000081")
                }
            val seed = TestSeed.parse(extras)
            withContext(io) { FileMockStateStore(root).save(original) }
            SettingsBoardCaptureFixture.prepare(root, seed, now, io)
            val stored = withContext(io) { checkNotNull(FileMockStateStore(root).load()) }
            val account = stored.accounts.values.single { it.snapshot.email == MockScenarioValues.demoEmail }
            val body = account.snapshot.settings.bodyClock
            val readiness = account.snapshot.readiness
            val initial = original.accounts.getValue(MockScenarioValues.demoID).snapshot
            if (!gated || state == "bodyReady") {
                assertEquals(initial.settings.bodyClock, body)
                assertEquals(initial.readiness, readiness)
            } else {
                when (state) {
                    "bodyMissing" -> {
                        assertEquals(JsonNull, body["ageBand"])
                        assertEquals(JsonNull, body["shiftWork"])
                        assertEquals(JsonPrimitive("needs-answers"), readiness["variant"])
                        assertEquals(2, (readiness["missing"] as kotlinx.serialization.json.JsonArray).size)
                    }

                    "bodyAdjusted" -> {
                        assertEquals(JsonPrimitive("65_plus"), body["ageBand"])
                        assertEquals(JsonPrimitive(true), body["excludeCaffeine"])
                        assertEquals(JsonPrimitive(true), body["flightMovementBreaks"])
                        assertEquals(JsonPrimitive("adjusted"), readiness["variant"])
                    }

                    "bodyExcluded" -> {
                        assertEquals(JsonPrimitive(true), body["shiftWork"])
                        assertEquals(JsonPrimitive("excluded"), readiness["variant"])
                    }
                }
            }
            assertTrue(
                stored.accounts.values.filter { it.snapshot.email != MockScenarioValues.demoEmail }.all {
                    it.snapshot == original.accounts[it.snapshot.id]?.snapshot
                },
            )
        }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-authTestId={1}")
        fun cases(): List<Array<Any>> =
            listOf("bodyMissing", "bodyReady", "bodyAdjusted", "bodyExcluded")
                .flatMap { state -> listOf(false, true).map { arrayOf(state, it) } }
    }
}
