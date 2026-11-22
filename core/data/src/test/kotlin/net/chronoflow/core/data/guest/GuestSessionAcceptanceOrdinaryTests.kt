package net.chronoflow.core.data.guest

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.data.wizard.MemberWizardStore
import net.chronoflow.core.model.auth.AuthenticationMethod
import net.chronoflow.core.model.launch.Gate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class GuestSessionAcceptanceOrdinaryTests(
    private val method: AuthenticationMethod,
) {
    @Test fun ordinaryAuthenticationKeepsTheMemberWizard() =
        runTest(timeout = 60.seconds) {
            val fixture = GuestAcceptanceFixture(StandardTestDispatcher(testScheduler))
            try {
                val result = fixture.result()
                fixture.controller.accept(result)
                val database = fixture.database()
                val store = MemberWizardStore(database, now = { fixture.clock.now }, io = fixture.io)
                val before = store.start()
                fixture.acceptance.accept(result, method, database)
                assertEquals(before, store.recover())
                assertNull(fixture.continuation().consume(Gate.App))
            } finally {
                fixture.close()
            }
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "method={0}")
        fun methods(): List<AuthenticationMethod> = AuthenticationMethod.entries
    }
}
