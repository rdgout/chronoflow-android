package net.chronoflow.feature.settings.delete

import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.feature.settings.testing.FakeSecretField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class SettingsDeleteFailureTests(
    private val failure: DataFailure,
    private val key: String,
) {
    @Test fun failurePreservesAccountAndConfirmationForRetry() =
        runTest(timeout = 60.seconds) {
            val field = FakeSecretField("password")
            val model = SettingsDeleteModel({ throw failure })
            model.confirmDeletion()
            model.submit(field)
            assertEquals(SettingsDeleteModel.Stage.Password, model.state.value.stage)
            assertTrue(field.cleared)
            assertFalse(model.state.value.busy)
            assertEquals(key, model.state.value.errorKey)
            model.stop()
            assertEquals(SettingsDeleteModel.Stage.Confirmation, model.state.value.stage)
            assertNull(model.state.value.errorKey)
        }

    @Test fun eachFailureMapsToTheIOSKey() =
        runTest(timeout = 60.seconds) {
            val model = SettingsDeleteModel({ throw failure })
            model.confirmDeletion()
            model.submit(FakeSecretField("pw"))
            assertEquals(key, model.state.value.errorKey)
        }

    companion object {
        private const val WRONG = "The provided password was incorrect."

        @JvmStatic
        @Parameterized.Parameters(name = "failure={0}-key={1}")
        fun cases() =
            listOf(
                arrayOf(DataFailure.Offline, "Needs a connection."),
                arrayOf(
                    DataFailure.Validation(mapOf("password" to FieldMessage(WRONG)), 422),
                    "The provided password was incorrect.",
                ),
                arrayOf(
                    DataFailure.Validation(mapOf("billing" to FieldMessage("billing")), 422),
                    "Your subscription could not be cancelled. Please try deleting your account again.",
                ),
                arrayOf(DataFailure.Server(503), "Something went wrong"),
                arrayOf(
                    DataFailure.Validation(mapOf("password" to FieldMessage("other")), 422),
                    "Check the highlighted field.",
                ),
                arrayOf(
                    DataFailure.Validation(
                        mapOf(
                            "billing" to FieldMessage("billing"),
                            "password" to FieldMessage(WRONG),
                        ),
                        422,
                    ),
                    "Your subscription could not be cancelled. Please try deleting your account again.",
                ),
            )
    }
}
