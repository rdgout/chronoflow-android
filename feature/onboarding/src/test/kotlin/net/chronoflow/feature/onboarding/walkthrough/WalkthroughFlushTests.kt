@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.feature.onboarding.walkthrough

import kotlinx.coroutines.test.runTest
import net.chronoflow.core.datamock.MockMethod
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.launch.Gate
import net.chronoflow.core.sync.SyncState
import net.chronoflow.feature.onboarding.support.OnboardingFixtureTest
import net.chronoflow.feature.onboarding.support.withOnboardingFixture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class WalkthroughFlushTests(
    private val failure: DataFailure,
) : OnboardingFixtureTest() {
    @Test fun failedFlushKeepsCompletionAndLaterSyncs() =
        runTest(timeout = 60.seconds) {
            withOnboardingFixture { f ->
                val model = f.walkthroughModel()
                model.skip()
                f.source.failNext(MockMethod.Send, failure)
                val engine = f.engine()
                try {
                    val failed = engine.refreshNow()
                    assertNotEquals(SyncState.Unauthorized, failed.state)
                    assertEquals(1, failed.pendingCount)
                    assertEquals(Gate.App, f.gate())
                    assertNotNull(f.controller.activeSession())
                    assertTrue(model.state.value.completed)
                    assertEquals(
                        1,
                        f.outbox
                            .pending()
                            .single()
                            .attempts,
                    )
                    assertEquals(0, engine.refreshNow().pendingCount)
                    assertTrue(f.source.fetchIdentity().walkthroughSeen)
                    assertEquals(Gate.App, f.gate())
                } finally {
                    engine.cancelAndWait()
                }
            }
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun failures() =
            listOf(
                DataFailure.Offline,
                DataFailure.Server(503),
                DataFailure.Unauthorized(401),
                DataFailure.Server(422),
            ).map {
                arrayOf(it)
            }
    }
}
