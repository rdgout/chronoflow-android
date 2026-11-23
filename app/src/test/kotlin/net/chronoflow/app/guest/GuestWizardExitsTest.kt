package net.chronoflow.app.guest

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.testing.wizard.generationReview
import net.chronoflow.core.wizard.model.MemberWizardDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException
import kotlin.time.Duration.Companion.seconds

class GuestWizardExitsTest {
    @Test fun closeDeletesSecretAndDraftAndShowsWelcome() =
        runTest(timeout = 60.seconds) {
            withGuest { draft ->
                exits().close(draft)
                assertEquals(Route.Welcome, navigator.state.value.route)
                assertFalse(file.exists())
                assertNull(secrets.get(draft.form.identity!!.token))
            }
        }

    @Test fun excludedCloseShowsWelcome() =
        runTest(timeout = 60.seconds) {
            withGuest { draft ->
                exits().close(draft)
                assertEquals(Route.Welcome, navigator.state.value.route)
            }
        }

    @Test fun foregroundAfterExpiryReturnsToWelcome() =
        runTest(timeout = 60.seconds) {
            withGuest { draft ->
                val exits = exits()
                exits.onForeground(draft) { }
                assertEquals(Route.GuestWizard, navigator.state.value.route)
                clock.advance(1860.0)
                exits.onForeground(draft) { }
                assertEquals(Route.Welcome, navigator.state.value.route)
                assertNull(secrets.get(draft.form.identity!!.token))
                assertFalse(file.exists())
            }
        }

    @Test fun startFailureReturnsToWelcome() =
        runTest(timeout = 60.seconds) {
            val f = GuestFixture(this)
            try {
                f.file.mkdirs()
                try {
                    f.dependencies.start()
                } catch (_: IOException) {
                }
                f.exits().close(null)
                assertEquals(Route.Welcome, f.navigator.state.value.route)
            } finally {
                f.root.deleteRecursively()
            }
        }

    @Test fun signedInStartAbortDoesNotNavigate() =
        runTest(timeout = 60.seconds) {
            withGuest { draft ->
                val exits = exits()
                presence = SessionPresence.SignedIn("member")
                val before = navigator.state.value
                exits.close(draft)
                assertEquals(before, navigator.state.value)
                assertNotNull(store.recover())
                assertNotNull(secrets.get(draft.form.identity!!.token))
            }
        }

    @Test fun staleRevisionCannotExitANewerGuest() =
        runTest(timeout = 60.seconds) {
            withGuest { draft ->
                val exits = exits()
                navigator.navigate(Route.GuestWizard)
                val before = navigator.state.value
                exits.close(draft)
                assertEquals(before, navigator.state.value)
                assertNotNull(store.recover())
            }
        }

    @Test fun redeliveredExitIsHarmless() =
        runTest(timeout = 60.seconds) {
            withGuest { draft ->
                val exits = exits()
                exits.close(draft)
                val revision = navigator.state.value.revision
                exits.close(draft)
                assertEquals(revision, navigator.state.value.revision)
            }
        }

    private suspend fun TestScope.withGuest(block: suspend GuestFixture.(MemberWizardDraft) -> Unit) {
        val f = GuestFixture(this)
        try {
            val base = f.store.start()
            val identity = generationReview().identity.copy(token = "guest")
            val draft = base.copy(form = base.form.copy(identity = identity))
            f.store.persist(draft)
            f.secrets.set("secret", identity.token)
            f.block(draft)
        } finally {
            f.root.deleteRecursively()
        }
    }
}
