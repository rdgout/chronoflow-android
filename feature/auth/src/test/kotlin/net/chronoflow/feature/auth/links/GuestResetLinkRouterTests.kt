package net.chronoflow.feature.auth.links

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.auth.ResetPasswordLink
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.model.session.SessionUpdate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class GuestResetLinkRouterTests {
    private val link = ResetPasswordLink("reset-token", "demo@example.com")

    @Test fun unresolvedLaunchWaitsForSignedOutRestore() =
        runTest(timeout = 60.seconds) {
            var presence: SessionPresence? = null
            val updates = MutableStateFlow<SessionUpdate?>(null)
            val opened = mutableListOf<ResetPasswordLink>()
            val scope = CoroutineScope(backgroundScope.coroutineContext + StandardTestDispatcher(testScheduler))
            val router = GuestResetLinkRouter({ presence }, updates, scope, opened::add)
            router.receive(link)
            assertEquals(link, router.pending)
            assertTrue(opened.isEmpty())
            presence = SessionPresence.SignedOut
            updates.value = SessionUpdate(presence, 1)
            runCurrent()
            assertEquals(listOf(link), opened)
            assertNull(router.pending)
        }

    @Test fun guestOpensImmediatelyAndDoesNotReplayOnLaterSessionUpdates() =
        runTest(timeout = 60.seconds) {
            val updates = MutableStateFlow<SessionUpdate?>(null)
            val opened = mutableListOf<ResetPasswordLink>()
            val scope = CoroutineScope(backgroundScope.coroutineContext + StandardTestDispatcher(testScheduler))
            val router = GuestResetLinkRouter({ SessionPresence.SignedOut }, updates, scope, opened::add)
            router.receive(link)
            assertEquals(listOf(link), opened)
            assertNull(router.pending)
            updates.value = SessionUpdate(SessionPresence.SignedOut, 2)
            runCurrent()
            val invalid = ResetPasswordLink("", "")
            router.receive(invalid)
            assertEquals(listOf(link, invalid), opened)
        }

    @Test fun activeSessionDefersLatestLinkUntilSignOutUnverified() = activeSessionDefersLatestLinkUntilSignOut()

    @Test fun activeSessionDefersLatestLinkUntilSignOutVerified() = activeSessionDefersLatestLinkUntilSignOut()

    private fun activeSessionDefersLatestLinkUntilSignOut() =
        runTest(timeout = 60.seconds) {
            val signedIn = SessionPresence.SignedIn("0199a3c0-0000-7000-8000-000000000001")
            var presence: SessionPresence = signedIn
            val updates = MutableStateFlow<SessionUpdate?>(SessionUpdate(presence, 1))
            val opened = mutableListOf<ResetPasswordLink>()
            val scope = CoroutineScope(backgroundScope.coroutineContext + StandardTestDispatcher(testScheduler))
            val router = GuestResetLinkRouter({ presence }, updates, scope, opened::add)
            router.receive(link)
            val latest = ResetPasswordLink("latest-token", link.email)
            router.receive(latest)
            assertTrue(opened.isEmpty())
            assertEquals(latest, router.pending)
            assertEquals(signedIn, presence)
            presence = SessionPresence.SignedOut
            updates.value = SessionUpdate(presence, 2)
            runCurrent()
            assertEquals(listOf(latest), opened)
            assertNull(router.pending)
            updates.value = SessionUpdate(presence, 3)
            runCurrent()
            assertEquals(1, opened.size)
        }

    @Test fun suspendedPresenceConsumesTheLatestLinkOnce() =
        runTest(timeout = 60.seconds) {
            val restored = CompletableDeferred<SessionPresence>()
            val updates = MutableStateFlow<SessionUpdate?>(null)
            val opened = mutableListOf<ResetPasswordLink>()
            val router = GuestResetLinkRouter({ restored.await() }, updates, backgroundScope, opened::add)
            val latest = ResetPasswordLink("latest-token", link.email)
            val first = launch { router.receive(link) }
            runCurrent()
            val second = launch { router.receive(latest) }
            runCurrent()
            restored.complete(SessionPresence.SignedOut)
            first.join()
            second.join()
            assertEquals(listOf(latest), opened)
            assertNull(router.pending)
        }
}
