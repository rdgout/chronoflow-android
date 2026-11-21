@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.app.guest

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.navigation.Route
import net.chronoflow.feature.launch.model.LaunchIntro
import net.chronoflow.feature.launch.model.ManualLaunchClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class GuestRecoverySchedulerTest {
    @Test fun waitsForTheLastIntroItem() =
        runTest(timeout = 60.seconds) {
            withRecovery { f ->
                val intro = LaunchIntro(2300.milliseconds, clock)
                val pending = backgroundScope.launch { scheduler.recover(true, intro) }
                runCurrent()
                clock.advance(3199.milliseconds)
                runCurrent()
                assertEquals(Route.Welcome, f.navigator.state.value.route)
                clock.advance(3200.milliseconds)
                pending.join()
                assertEquals(Route.GuestWizard, f.navigator.state.value.route)
            }
        }

    @Test fun noIntroRecoversAtOnce() =
        runTest(timeout = 60.seconds) {
            withRecovery { f ->
                scheduler.recover(true, null)
                assertEquals(Route.GuestWizard, f.navigator.state.value.route)
            }
        }

    @Test fun frozenIntroNeverRecovers() =
        runTest(timeout = 60.seconds) {
            withRecovery { f ->
                scheduler.recover(true, LaunchIntro(2300.milliseconds, clock, 2300.milliseconds))
                assertEquals(Route.Welcome, f.navigator.state.value.route)
                assertEquals(0, clock.pendingSleeps)
            }
        }

    @Test fun signingInOpensSignIn() =
        runTest(timeout = 60.seconds) {
            withRecovery { f ->
                val draft = checkNotNull(f.store.recover())
                f.store.persist(draft.copy(state = draft.state.copy(signingIn = true)))
                scheduler.recover(true, null)
                assertEquals(Route.SignIn, f.navigator.state.value.route)
            }
        }

    @Test fun expiredDraftOpensNothingAndIsDeleted() =
        runTest(timeout = 60.seconds) {
            withRecovery { f ->
                f.clock.advance(1800.0)
                scheduler.recover(true, null)
                assertEquals(Route.Welcome, f.navigator.state.value.route)
                assertFalse(f.file.exists())
            }
        }

    @Test fun navigationDuringTheIntroCancels() =
        runTest(timeout = 60.seconds) {
            withRecovery { f ->
                val pending = backgroundScope.launch { scheduler.recover(true, LaunchIntro(2300.milliseconds, clock)) }
                runCurrent()
                clock.advance(2800.milliseconds)
                f.navigator.navigate(Route.SignIn)
                f.navigator.navigate(Route.Welcome)
                val before = f.navigator.state.value
                clock.advance(3200.milliseconds)
                pending.join()
                assertEquals(before, f.navigator.state.value)
            }
        }

    @Test fun signedInNeverRecovers() =
        runTest(timeout = 60.seconds) {
            withRecovery { f ->
                f.presence = SessionPresence.SignedIn("member")
                scheduler.recover(true, null)
                assertEquals(Route.Welcome, f.navigator.state.value.route)
            }
        }

    @Test fun runsOncePerProcess() =
        runTest(timeout = 60.seconds) {
            withRecovery { f ->
                scheduler.recover(true, null)
                f.navigator.navigate(Route.Welcome)
                val before = f.navigator.state.value
                scheduler.recover(true, null)
                assertEquals(before, f.navigator.state.value)
            }
        }

    @Test fun epochChangeCancels() =
        runTest(timeout = 60.seconds) {
            withRecovery { f ->
                val pending = backgroundScope.launch { scheduler.recover(true, LaunchIntro(2300.milliseconds, clock)) }
                runCurrent()
                epoch++
                clock.advance(3200.milliseconds)
                pending.join()
                assertEquals(Route.Welcome, f.navigator.state.value.route)
            }
        }

    @Test fun unresolvedSessionDoesNotConsumeRecovery() =
        runTest(timeout = 60.seconds) {
            withRecovery { f ->
                f.presence = null
                scheduler.recover(true, null)
                assertEquals(Route.Welcome, f.navigator.state.value.route)
                f.presence = SessionPresence.SignedOut
                scheduler.recover(true, null)
                assertEquals(Route.GuestWizard, f.navigator.state.value.route)
            }
        }

    private class RecoveryFixture(
        f: GuestFixture,
    ) {
        val clock = ManualLaunchClock()
        var epoch = 1L
        val scheduler = GuestRecoveryScheduler(f.dispatchers, f.store, f.navigator, { f.presence }, { epoch })
    }

    private suspend fun TestScope.withRecovery(block: suspend RecoveryFixture.(GuestFixture) -> Unit) {
        val f = GuestFixture(this)
        try {
            f.navigator.navigate(Route.Welcome)
            f.store.start()
            RecoveryFixture(f).block(f)
        } finally {
            f.root.deleteRecursively()
        }
    }
}
