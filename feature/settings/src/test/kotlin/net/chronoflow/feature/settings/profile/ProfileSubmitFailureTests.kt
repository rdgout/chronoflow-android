@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.settings.profile

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.device.DeviceIdentityFailure
import net.chronoflow.core.model.session.SessionFailure
import net.chronoflow.core.model.store.LocalMutationFailure
import net.chronoflow.core.model.store.StoreFailure
import net.chronoflow.feature.settings.support.FakeSettingsDependencies
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.IOException
import java.security.GeneralSecurityException
import java.sql.SQLException
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class ProfileSubmitFailureTests(
    private val name: String,
    private val failure: Throwable,
) {
    @Test fun submitFailureKeepsTheDraftAndCanRetry() =
        runTest(timeout = 60.seconds) {
            val deps = FakeSettingsDependencies().apply { save = { _, _ -> throw failure } }
            withProfile(deps) { vm ->
                vm.edit("Edited")
                vm.submit()
                runCurrent()
                assertEquals(
                    name,
                    ProfileError.CouldNotSave,
                    vm.ui.value.editor
                        ?.error,
                )
                assertEquals(
                    "Edited",
                    vm.ui.value.editor
                        ?.name,
                )
                assertFalse(
                    vm.ui.value.editor!!
                        .busy,
                )
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
            )
    }
}
