package net.chronoflow.feature.auth.model

import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.OutboxKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Enclosed::class)
class AuthFormModelTests {
    class Behavior {
        @Test fun oneLockCoversTapReturnAndPasskeyAndEditsClearFeedback() {
            val form = AuthFormModel()
            assertTrue(form.begin(AuthFormModel.Action.Password))
            assertFalse(form.begin(AuthFormModel.Action.Password))
            assertFalse(form.begin(AuthFormModel.Action.Passkey))
            form.receive(fillFailure)
            form.edit("email")
            form.showFields(emptySet())
            assertNotNull(form.state.value.fields["email"])
            form.finish()
            form.edit("email")
            assertTrue(
                form.state.value.fields
                    .isEmpty(),
            )
            assertTrue(form.begin(AuthFormModel.Action.Passkey))
            assertNull(form.state.value.failure)
            form.finish()
            form.showFields(emptySet())
            form.receive(fillFailure)
            assertEquals(AuthBannerKind.Server, form.state.value.banner)
        }

        @Test fun announcementsAreConsumedOncePerFailureAndNeverRepeatOnEdit() {
            val form = AuthFormModel()
            form.receive(DataFailure.Server(500))
            assertEquals("Something went wrong on our side. Try again in a moment.", form.takeAnnouncement { it.key })
            assertNull(form.takeAnnouncement { it.key })
            form.edit("email")
            assertNull(form.takeAnnouncement { it.key })
            form.receive(DataFailure.Server(500))
            assertNotNull(form.takeAnnouncement { it.key })
            form.receive(DataFailure.Offline)
            assertEquals(DataFailure.Offline, form.state.value.failure)
            assertEquals(AuthBannerKind.Offline, form.state.value.banner)
            assertEquals("Needs a connection", form.takeAnnouncement { it.key })
            assertTrue(form.begin(AuthFormModel.Action.Password))
        }

        @Test fun fieldOnlyAnnouncementsClearOnEditAndUseTheLatestFailure() {
            val form = AuthFormModel()
            form.receive(fillFailure)
            assertNull(form.state.value.banner)
            assertEquals("Please fill this in.", form.takeAnnouncement { it.key })
            form.receive(fillFailure)
            form.edit("email")
            assertNull(form.takeAnnouncement { it.key })
            form.receive(fillFailure)
            form.receive(DataFailure.Offline)
            assertEquals("Needs a connection", form.takeAnnouncement { it.key })
            form.showFields(setOf("password", "email"))
            form.receive(
                DataFailure.Validation(
                    mapOf(
                        "password" to FieldMessage("Use at least one letter."),
                        "email" to FieldMessage("Please fill this in."),
                    ),
                    422,
                ),
            )
            assertEquals("Please fill this in. Use at least one letter.", form.takeAnnouncement { it.key })
        }
    }

    @RunWith(Parameterized::class)
    class Failures(
        private val case: AuthFormFailureCase,
    ) {
        @Test fun everyFailureHasVisibleLocalizedFeedbackAndOneAnnouncement() {
            val form = AuthFormModel(case.fields, case.context)
            assertTrue(form.begin(AuthFormModel.Action.Password))
            form.receive(case.failure)
            form.finish()
            val state = form.state.value
            assertEquals(case.failure, state.failure)
            assertFalse(state.busy)
            assertTrue(case.fields.containsAll(state.fields.keys))
            val visible = state.fields.values.mapNotNull(AuthFieldCopy::field)
            assertEquals(state.fields.size, visible.size)
            assertTrue(state.banner != null || visible.isNotEmpty())
            val announcement = form.takeAnnouncement { it.key }
            assertNotNull(announcement)
            assertTrue(visible.all { announcement!!.contains(it.key) })
            state.banner?.let { assertTrue(announcement!!.contains(it.title(case.context))) }
            assertNull(form.takeAnnouncement { it.key })
            form.receive(case.failure)
            assertEquals(announcement, form.takeAnnouncement { it.key })
            form.clear()
            assertNull(form.takeAnnouncement { it.key })
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun cases(): List<Array<Any>> {
                val failures =
                    listOf(
                        DataFailure.ReviewExpired,
                        DataFailure.NotEnoughCredits,
                        DataFailure.SaveCapacity,
                        DataFailure.ProfileInvalid(mapOf("name" to fill)),
                        DataFailure.Offline,
                        DataFailure.Unauthorized(401),
                        DataFailure.EmailUnverified(403),
                        DataFailure.Validation(emptyMap(), 422),
                        validation("name", fill),
                        validation("email", FieldMessage("This email address is already in use.")),
                        validation("email", FieldMessage("These sign-in details don't match our records.")),
                        validation("email", fill),
                        validation("password", fill),
                        validation("password_confirmation", FieldMessage("The passwords don't match yet.")),
                        validation("code", FieldMessage("The authentication code was not accepted. Try again.")),
                        validation("recovery_code", fill),
                        validation("hidden", fill),
                        validation("email", FieldMessage("Unknown server copy")),
                        validation("password", FieldMessage("Use at least :min characters.")),
                        DataFailure.Throttled(60.0, 429),
                        DataFailure.Conflict(409),
                        DataFailure.Server(500),
                        DataFailure.Server(null),
                        DataFailure.Unsupported(OutboxKind.Security),
                    )
                val contexts =
                    listOf(
                        AuthFormContext.SignIn to setOf("email", "password"),
                        AuthFormContext.SignUp to setOf("name", "email", "password", "password_confirmation"),
                        AuthFormContext.Forgot to setOf("email"),
                        AuthFormContext.Reset to setOf("password", "password_confirmation"),
                        AuthFormContext.TwoFactor to setOf("code"),
                        AuthFormContext.TwoFactor to setOf("recovery_code"),
                        AuthFormContext.Verify to emptySet(),
                    )
                return failures.flatMapIndexed {
                    index,
                    failure,
                    ->
                    contexts.map { (context, fields) ->
                        arrayOf(AuthFormFailureCase(index, failure, context, fields))
                    }
                }
            }
        }
    }
}

class AuthFormFailureCase(
    val index: Int,
    val failure: DataFailure,
    val context: AuthFormContext,
    val fields: Set<String>,
) {
    override fun toString(): String = "failure-$index-$context-${fields.sorted().joinToString("+")}"
}

private val fill = FieldMessage("Please fill this in.")
private val fillFailure = DataFailure.Validation(mapOf("email" to fill), 422)

private fun validation(
    field: String,
    message: FieldMessage,
) = DataFailure.Validation(mapOf(field to message), 422)
