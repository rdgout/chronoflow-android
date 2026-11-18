package net.chronoflow.core.datamock

import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class SettingsDeleteAPITests {
    @Test fun mockDeletionRequiresPasswordAndActuallyRemovesAccount() =
        runTest(timeout = 60.seconds) {
            val source = mockSource()
            val session = login(source)
            assertEquals(
                DataFailure.Validation(
                    mapOf("password" to FieldMessage("The provided password was incorrect.")),
                    422,
                ),
                failure {
                    source.deleteAccount("wrong")
                },
            )
            assertEquals(session.account.id, source.fetchIdentity().id)
            source.setOffline(true)
            assertEquals(DataFailure.Offline, failure { source.deleteAccount("password") })
            source.setOffline(false)
            assertEquals(session.account.id, source.fetchIdentity().id)
            source.deleteAccount("password")
            assertEquals(DataFailure.Unauthorized(401), failure { source.fetchIdentity() })
        }
}
