@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.debug

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.testing.wizard.GenerationSource
import net.chronoflow.core.testing.wizard.InMemoryWizardDraftStore
import net.chronoflow.core.wizard.generation.TripGenerationEffects
import net.chronoflow.core.wizard.generation.TripGenerationService
import net.chronoflow.core.wizard.model.AirportSearchService
import net.chronoflow.core.wizard.model.MemberWizardDraft
import net.chronoflow.core.wizard.model.TripWizardAction
import net.chronoflow.core.wizard.model.WizardAirportSheet
import net.chronoflow.feature.wizard.TripWizardDependencies
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

class WizardDebugSeedsTest {
    private fun seed(
        auto: Boolean = false,
        board: String? = null,
    ) = TestSeed(
        testId = "wizard-test",
        profile = UiTestProfile.Screen,
        theme = null,
        language = null,
        reduceMotion = null,
        holdSplash = null,
        freezeAtMs = null,
        freezeAtRaw = null,
        readyAtMs = null,
        mainThreadStallMs = null,
        raw = emptyMap(),
        ignoredKeys = emptySet(),
        wizardAutoOpen = auto,
        wizardBoardCapture = board,
    )

    private class Dependencies(
        scope: TestScope,
    ) : TripWizardDependencies {
        override val store = InMemoryWizardDraftStore()
        override val now = { Instant.EPOCH }
        override val source = GenerationSource()
        override val writerScope = scope.backgroundScope
        override val airportSearch = AirportSearchService({ emptyList() }, now, {})

        override suspend fun start() = MemberWizardDraft()

        override suspend fun startTrip(
            id: String,
            action: TripWizardAction,
        ) = MemberWizardDraft()

        override fun newGeneration() =
            TripGenerationService(
                source,
                TripGenerationEffects({
                }, {}, {}),
                parentContext = writerScope.coroutineContext,
            )
    }

    @Test fun autoOpenRunsOncePerEpoch() {
        val seeds = WizardDebugSeeds()
        val seed = seed(auto = true)
        assertTrue(seeds.prepare(seed, 1, Locale.ENGLISH).open)
        assertFalse(seeds.prepare(seed, 1, Locale.ENGLISH).open)
        assertTrue(seeds.prepare(seed, 2, Locale.ENGLISH).open)
    }

    @Test fun stopSearchSecondInstallsOnlyMemoryStateAndTheFixedClock() =
        runTest(timeout = 60.seconds) {
            val seeds = WizardDebugSeeds()
            val prepared = seeds.prepare(seed(board = "WizardStopSearchSecond"), 1, Locale.ENGLISH)
            assertTrue(prepared.open)
            val override = checkNotNull(prepared.override)
            val base = Dependencies(this)
            val deps = override.dependencies(base)
            val draft = deps.start()
            assertEquals(WizardAirportSheet(1, WizardAirportSheet.Side.stop), draft.state.sheet)
            assertEquals("Ban", override.presentation.query)
            assertEquals(listOf("BKK", "DMK"), deps.airportSearch.results("Ban").map { it.iata })
            assertEquals(Instant.parse("2026-10-01T09:00:00Z"), deps.now())
            deps.store.persist(draft)
            assertTrue(base.store.persisted.isEmpty())
            assertNotNull(seeds.prepare(seed(board = "WizardStopSearchSecond"), 1, Locale.ENGLISH).override)
        }

    @Test fun captureSearchCanAcceptAnotherQueryWithoutMovingTheWizardClock() =
        runTest(timeout = 60.seconds) {
            val override =
                checkNotNull(WizardDebugSeeds().prepare(seed(board = "WizardStopSearch"), 1, Locale.ENGLISH).override)
            val deps = override.dependencies(Dependencies(this))
            assertEquals(listOf("DXB", "DWC", "DUS", "DUB"), deps.airportSearch.results("Du").map { it.iata })
            assertEquals(listOf("BKK", "DMK"), deps.airportSearch.results("Ban").map { it.iata })
            assertEquals(Instant.parse("2026-10-01T09:00:00Z"), deps.now())
        }

    @Test fun tripActionsDoesNotOverrideOrOpenTheWizard() {
        val prepared = WizardDebugSeeds().prepare(seed(board = "TripActions"), 1, Locale.ENGLISH)
        assertFalse(prepared.open)
        assertNull(prepared.override)
    }

    @Test fun completionBoardInstallsTheFrozenClockAndUiOverride() =
        runTest(timeout = 60.seconds) {
            val prepared = WizardDebugSeeds().prepare(seed(board = "WizardCheckThrottle"), 1, Locale.ENGLISH)
            val deps = checkNotNull(prepared.override).dependencies(Dependencies(this))
            assertTrue(prepared.open)
            assertEquals(42, deps.uiOverride?.retrySeconds)
            assertEquals(2, deps.uiOverride?.checking?.ticks)
            org.junit.Assert.assertSame(
                net.chronoflow.feature.wizard.debug.FrozenCheckingClock.clock,
                deps.checkingClock,
            )
            assertEquals(0L, deps.checkingClock.nowMillis())
        }
}
