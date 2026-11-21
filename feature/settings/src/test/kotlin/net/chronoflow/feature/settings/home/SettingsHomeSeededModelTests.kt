@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.settings.home

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.data.repository.OutboxRepository
import net.chronoflow.core.data.repository.SettingsRepository
import net.chronoflow.core.data.repository.TripRepository
import net.chronoflow.core.datamock.MockDataSource
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.model.store.SettingsRecord
import net.chronoflow.feature.settings.support.SettingsFixture
import net.chronoflow.feature.settings.support.SettingsFixtureTest
import net.chronoflow.feature.settings.support.withSettingsFixture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class SettingsHomeSeededModelTests(
    private val mutationsOffline: Boolean,
) : SettingsFixtureTest() {
    @Test fun realSeededCheckOffRequiresConfirmationOnlyWhileMutationsStayOffline() =
        runTest(timeout = 60.seconds) {
            withSettingsFixture(mutationsOffline) { f ->
                val model = f.model()
                f.pending()
                runCurrent()
                model.signOut()
                runCurrent()
                assertNull(model.failure)
                assertEquals(mutationsOffline, model.ui.value.showsPending)
                if (mutationsOffline) {
                    assertEquals(1, model.ui.value.confirmationCount)
                    assertEquals(
                        "offline",
                        f.outbox
                            .pending()
                            .single()
                            .lastError,
                    )
                    model.stay()
                    assertEquals(f.snapshot, f.controller.activeSession())
                    model.signOut()
                    runCurrent()
                    assertEquals(
                        2,
                        f.outbox
                            .pending()
                            .single()
                            .attempts,
                    )
                    model.signOut(true)
                    runCurrent()
                }
                assertEquals(SessionPresence.SignedOut, f.controller.presence)
            }
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "mutationsOffline={0}")
        fun cases(): Collection<Array<Any>> = listOf(arrayOf(false), arrayOf(true))
    }
}
