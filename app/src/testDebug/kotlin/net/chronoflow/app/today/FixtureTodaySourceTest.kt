@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.app.today

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.app.StubConfig
import net.chronoflow.app.StubSession
import net.chronoflow.app.perf.LaunchDiagnosticsParser
import net.chronoflow.app.perf.StubReadinessFactory
import net.chronoflow.app.perf.today.FixtureTodaySource
import net.chronoflow.app.todayStubSelected
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.launch.Gate
import net.chronoflow.core.model.launch.SignedOutReadiness
import net.chronoflow.core.plan.PlanDecoder
import net.chronoflow.core.plan.selectTodayTrip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

class FixtureTodaySourceTest {
    @Test fun signedInPerfStubCanLeaveLoadingAndCheckOff() =
        runTest(timeout = 60.seconds) {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val now = Instant.parse("2026-11-19T10:00:00Z")
            val source = FixtureTodaySource(dispatcher) { now }
            assertTrue(source.synced().first())
            source.preferences().first()
            val trips = PlanDecoder(dispatcher).trips(source.trips().first(), now)
            assertEquals(null, trips.error)
            val selected = selectTodayTrip(trips.values, now)!!
            val detail = source.detail(selected.card.id).first()!!
            assertNotNull(detail.plan.days.firstOrNull())
            val block = detail.plan.blocks.first { it.actionable && it.checkKey != null }
            source.setCheck(selected.card.id, detail, block.id, CheckStatus.Done)
            val marked = source.detail(selected.card.id).first()!!
            assertTrue(marked.checkState.marks.any { it.key == block.checkKey && it.status == CheckStatus.Done })
            source.setCheck(selected.card.id, marked, block.id, null)
            assertFalse(
                source
                    .detail(selected.card.id)
                    .first()!!
                    .checkState.marks
                    .any { it.key == block.checkKey },
            )
        }

    @Test fun onlyAnExplicitSignedInPerfAppStubSelectsTheFixture() {
        val readiness = LaunchDiagnosticsParser.overrides(mapOf("cf.session" to "in", "cf.gate" to "app")).readiness
        assertTrue(todayStubSelected(true, readiness))
        assertFalse(todayStubSelected(false, readiness))
        assertFalse(todayStubSelected(true, null))
        assertFalse(todayStubSelected(true, { SignedOutReadiness }))
    }

    @Test fun signedOutStubDoesNotSelectTheFixture() {
        assertFalse(
            todayStubSelected(
                true,
                StubReadinessFactory(StubConfig(session = StubSession.SignedOut), SignedOutReadiness),
            ),
        )
    }

    @Test fun nativeSessionStubDoesNotSelectTheFixture() {
        assertFalse(todayStubSelected(true, StubReadinessFactory(StubConfig(), SignedOutReadiness)))
    }

    @Test fun gateScreenDoesNotSelectTheFixture() {
        assertFalse(
            todayStubSelected(
                true,
                StubReadinessFactory(
                    StubConfig(session = StubSession.SignedIn, gate = Gate.Walkthrough),
                    SignedOutReadiness,
                ),
            ),
        )
    }
}
