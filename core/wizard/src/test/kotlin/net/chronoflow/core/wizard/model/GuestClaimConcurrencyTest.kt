package net.chronoflow.core.wizard.model

import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.contracts.ApiReviewIdentity
import net.chronoflow.core.model.guest.GuestClaim
import net.chronoflow.core.testing.wizard.WizardTestClock
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class GuestClaimConcurrencyTest {
    @Test fun reviewExpiryCannotOverwriteAConcurrentReview() =
        runTest(timeout = 60.seconds) {
            val root = File("build/guest-claim-tests/${UUID.randomUUID()}")
            val file = File(root, "guest-wizard.json")
            val clock = WizardTestClock()
            val io = StandardTestDispatcher(testScheduler)
            lateinit var store: GuestWizardStore
            lateinit var replacement: MemberWizardDraft
            var armed = false
            var writer: Job? = null
            store =
                GuestWizardStore({
                    if (armed) {
                        armed = false
                        writer = backgroundScope.launch(io) { store.persist(replacement) }
                    }
                    file
                }, io, { clock.now })
            try {
                val initial = store.start()
                val oldIdentity = ApiReviewIdentity("old", 1, 2, null, null)
                val draft = initial.copy(form = initial.form.copy(identity = oldIdentity))
                store.persist(draft)
                replacement =
                    draft.copy(
                        form = draft.form.copy(identity = ApiReviewIdentity("new", 2, 3, null, null)),
                        state = draft.state.copy(revision = draft.state.revision + 1),
                    )
                armed = true
                store.reviewExpired(GuestClaim("old", 1, 2, "secret"))
                checkNotNull(writer).join()
                assertEquals(replacement, store.recover())
            } finally {
                withContext(NonCancellable) {
                    writer?.cancelAndJoin()
                    root.deleteRecursively()
                }
            }
        }
}
