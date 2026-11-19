package net.chronoflow.app.links

import net.chronoflow.app.PendingLaunchIntents
import net.chronoflow.core.model.auth.AuthLink
import net.chronoflow.core.testing.AuthLinkUrls
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthLinkDispatcherTest {
    private val pending = PendingLaunchIntents()
    private val handled = mutableListOf<AuthLink>()
    private val dispatcher = AuthLinkDispatcher(pending, handled::add)
    private val first = AuthLinkUrls.reset("first", "a@b.c")
    private val last = AuthLinkUrls.reset("last", "a@b.c")

    @Test fun captureBeforeAttachWaitsForAttach() {
        pending.captureLink(first)
        dispatcher.deliver()
        assertTrue(handled.isEmpty())
        dispatcher.attach()
        assertEquals(listOf(AuthLink.parse(first)), handled)
    }

    @Test fun twoCapturedLinksDeliverOnlyTheLatest() {
        pending.captureLink(first)
        pending.captureLink(last)
        dispatcher.attach()
        assertEquals(listOf(AuthLink.parse(last)), handled)
        assertNull(pending.consume())
    }

    @Test fun recapturingAnEqualStringMovesItToTheEnd() {
        pending.captureLink(first)
        pending.captureLink(last)
        pending.captureLink(first)
        dispatcher.attach()
        assertEquals(listOf(AuthLink.parse(first)), handled)
    }

    @Test fun attachTwiceDeliversOnce() {
        pending.captureLink(first)
        dispatcher.attach()
        dispatcher.attach()
        assertEquals(1, handled.size)
    }

    @Test fun nonAuthLinksStayForTheExistingHandler() {
        val other = "https://app.chronoflow.net/x"
        pending.captureLink(other)
        pending.captureLink(first)
        dispatcher.attach()
        assertEquals(listOf(AuthLink.parse(first)), handled)
        assertEquals(setOf(other), pending.consume()!!.links)
    }
}
