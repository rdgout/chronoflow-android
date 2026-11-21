@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.settings.delete

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.device.DeviceIdentityFailure
import net.chronoflow.core.model.session.SessionFailure
import net.chronoflow.core.model.session.StaleSessionException
import net.chronoflow.core.model.store.LocalMutationFailure
import net.chronoflow.core.model.store.StoreFailure
import net.chronoflow.feature.settings.support.FakeSettingsDependencies
import net.chronoflow.feature.settings.testing.FakeSecretField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.IOException
import java.security.GeneralSecurityException
import java.sql.SQLException
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class DeleteAccountViewModelFailureTest(
    private val name: String,
    private val failure: Throwable,
) {
    @Test fun dependencyFailureIsMappedWithoutEscapingTheSubmission() =
        runTest(timeout = 60.seconds) {
            val deps = FakeSettingsDependencies(backgroundScope).apply { delete = { throw failure } }
            withDeleteAccountModel(deps) { vm ->
                vm.event(DeleteAccountEvent.Confirm)
                val field = FakeSecretField("pw")
                vm.event(DeleteAccountEvent.Submit(field))
                runCurrent()
                assertTrue(field.cleared)
                assertFalse(vm.ui.value.busy)
                assertEquals(SettingsDeleteModel.Stage.Password, vm.ui.value.delete.stage)
                val key =
                    when (failure) {
                        is StaleSessionException -> null
                        DataFailure.Offline -> "Needs a connection."
                        else -> "Something went wrong"
                    }
                assertEquals(name, key, vm.ui.value.delete.errorKey)
            }
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> =
            listOf(
                arrayOf("session", SessionFailure.CredentialsUnavailable),
                arrayOf("data", DataFailure.Offline),
                arrayOf("identity", DeviceIdentityFailure.InvalidPersistedIdentity),
                arrayOf("store", StoreFailure.Closed),
                arrayOf("local", LocalMutationFailure.MissingAccount),
                arrayOf("io", IOException()),
                arrayOf("security", GeneralSecurityException()),
                arrayOf("sql", SQLException()),
                arrayOf("runtime", IllegalStateException()),
                arrayOf("foreign-cancellation", CancellationException()),
                arrayOf("stale-session", StaleSessionException()),
            )
    }
}
