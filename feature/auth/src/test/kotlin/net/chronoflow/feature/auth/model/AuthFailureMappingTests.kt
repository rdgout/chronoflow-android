package net.chronoflow.feature.auth.model

import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Enclosed::class)
class AuthFailureMappingTests {
    class Mapping {
        @Test fun onlyApprovedMessagesReachDisplayedFields() {
            val mapped =
                AuthFailureMapping(
                    DataFailure.Validation(
                        mapOf(
                            "email" to fill,
                            "password" to FieldMessage("unknown server English"),
                            "device_id" to fill,
                        ),
                        422,
                    ),
                    setOf("email", "password"),
                )
            assertEquals(mapOf("email" to fill), mapped.fields)
            assertEquals(AuthBannerKind.Server, mapped.banner)
            val credentials =
                AuthFailureMapping(
                    DataFailure.Validation(
                        mapOf(
                            "email" to FieldMessage("These sign-in details don't match our records."),
                            "password" to fill,
                        ),
                        422,
                    ),
                    setOf("email", "password"),
                )
            assertEquals(AuthBannerKind.Credentials, credentials.banner)
            assertTrue(credentials.fields.isEmpty())
            val taken =
                AuthFailureMapping(
                    DataFailure.Validation(
                        mapOf("email" to FieldMessage("This email address is already in use."), "password" to fill),
                        422,
                    ),
                    setOf("email", "password"),
                    AuthFormContext.SignUp,
                )
            assertEquals(AuthBannerKind.EmailTaken, taken.banner)
            assertEquals(mapOf("password" to fill), taken.fields)
        }

        @Test fun offlineUsesTheRNBannerAndThrottleRetainsDeadline() {
            assertEquals(AuthBannerKind.Offline, AuthFailureMapping(DataFailure.Offline, emptySet()).banner)
            assertEquals("", AuthBannerKind.Offline.body())
            val throttle = AuthFailureMapping(DataFailure.Throttled(73.0, 429), emptySet())
            assertEquals(AuthBannerKind.Locked, throttle.banner)
            assertEquals(73.0, throttle.retryAfterSeconds!!, 0.0)
        }

        @Test fun obsoleteCredentialsKeyUsesTheServerFallback() {
            val mapped =
                AuthFailureMapping(
                    DataFailure.Validation(
                        mapOf("email" to FieldMessage("These credentials do not match our records.")),
                        422,
                    ),
                    setOf("email", "password"),
                )
            assertEquals(AuthBannerKind.Server, mapped.banner)
            assertTrue(mapped.fields.isEmpty())
        }
    }

    @RunWith(Parameterized::class)
    class Proof(
        private val field: String,
    ) {
        @Test fun inactiveProofIsIgnoredOnlyAlongsideAnActiveProofError() {
            val inactive = if (field == "code") "recovery_code" else "code"
            val both = mapOf(field to fill, inactive to fill)
            val mapped = AuthFailureMapping(DataFailure.Validation(both, 422), setOf(field), AuthFormContext.TwoFactor)
            assertEquals(mapOf(field to fill), mapped.fields)
            assertNull(mapped.banner)
            assertEquals(
                AuthBannerKind.Server,
                AuthFailureMapping(
                    DataFailure.Validation(mapOf(inactive to fill), 422),
                    setOf(field),
                    AuthFormContext.TwoFactor,
                ).banner,
            )
            assertEquals(
                AuthBannerKind.Server,
                AuthFailureMapping(
                    DataFailure.Validation(both + ("challengeId" to fill), 422),
                    setOf(field),
                    AuthFormContext.TwoFactor,
                ).banner,
            )
            assertEquals(
                AuthBannerKind.Server,
                AuthFailureMapping(DataFailure.Validation(both, 422), setOf(field)).banner,
            )
            assertEquals(
                AuthBannerKind.Server,
                AuthFailureMapping(
                    DataFailure.Validation(both + (field to FieldMessage("unknown")), 422),
                    setOf(field),
                    AuthFormContext.TwoFactor,
                ).banner,
            )
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun cases() = listOf("code", "recovery_code")
        }
    }

    @RunWith(Parameterized::class)
    class RejectedCode(
        private val context: AuthFormContext,
        private val field: String,
    ) {
        @Test fun rejectedCodeUsesBoardCopyOnlyForTheTwoFactorCodeField() {
            val message = FieldMessage("The authentication code was not accepted. Try again.")
            val mapped = AuthFailureMapping(DataFailure.Validation(mapOf(field to message), 422), setOf(field), context)
            assertNull(mapped.banner)
            assertEquals(
                if (context == AuthFormContext.TwoFactor &&
                    field == "code"
                ) {
                    "That code didn't work. Codes change every 30 seconds, so try the newest one."
                } else {
                    message.key
                },
                mapped.fields[field]?.key,
            )
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{0}-{1}")
            fun cases() =
                listOf(
                    arrayOf<Any>(AuthFormContext.TwoFactor, "code"),
                    arrayOf<Any>(AuthFormContext.TwoFactor, "recovery_code"),
                    arrayOf<Any>(AuthFormContext.SignIn, "code"),
                )
        }
    }

    @RunWith(Parameterized::class)
    class Arguments(
        private val key: String,
        private val args: Map<String, JsonPrimitive>,
        private val expected: AuthText?,
    ) {
        @Test fun fieldArgumentsAreWhitelistedAndNeverExposeUnknownEnglish() {
            val message = FieldMessage(key, args)
            assertEquals(expected, AuthFieldCopy.field(message))
            assertEquals(expected != null, AuthFieldCopy.supports(message))
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "argument-{index}")
            fun cases(): List<Array<Any?>> =
                listOf(
                    arrayOf(
                        "Use no more than :max characters.",
                        mapOf("max" to JsonPrimitive(255)),
                        AuthText("Use no more than %lld characters.", 255),
                    ),
                    arrayOf(
                        "Use no more than {{max}} characters.",
                        mapOf("max" to JsonPrimitive(255)),
                        AuthText("Use no more than %lld characters.", 255),
                    ),
                    arrayOf(
                        "Use at least :min characters.",
                        mapOf("min" to JsonPrimitive(12)),
                        AuthText("Use at least %lld characters.", 12),
                    ),
                    arrayOf(
                        "Use at least {{min}} characters.",
                        mapOf("min" to JsonPrimitive(0)),
                        AuthText("Use at least %lld characters.", 0),
                    ),
                    arrayOf("Use no more than :max characters.", mapOf("max" to JsonPrimitive("255")), null),
                    arrayOf("Use no more than :max characters.", mapOf("max" to JsonPrimitive(2.5)), null),
                    arrayOf("Use no more than :max characters.", mapOf("max" to JsonPrimitive(-1)), null),
                    arrayOf("Use no more than :max characters.", emptyMap<String, JsonPrimitive>(), null),
                    arrayOf(
                        "Use at least :min characters.",
                        mapOf("min" to JsonPrimitive(Long.MAX_VALUE)),
                        AuthText("Use at least %lld characters.", Long.MAX_VALUE),
                    ),
                    arrayOf(
                        "Use at least :min characters.",
                        mapOf("min" to JsonPrimitive(java.math.BigInteger("9223372036854775808"))),
                        null,
                    ),
                )
        }
    }

    @RunWith(Parameterized::class)
    class Backend(
        private val key: String,
        private val field: String,
    ) {
        @Test fun backendFieldMessagesMapAndLocalizeInBothLanguages() {
            val message = FieldMessage(key)
            val failure = DataFailure.Validation(mapOf(field to message), 422)
            val mapped = AuthFailureMapping(failure, setOf("email", "password"))
            assertTrue(AuthFieldCopy.supports(message))
            assertEquals(mapOf(field to message), mapped.fields)
            assertNull(mapped.banner)
            val hidden = AuthFailureMapping(failure, emptySet())
            assertTrue(hidden.fields.isEmpty())
            assertEquals(AuthBannerKind.Server, hidden.banner)
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun cases() =
                listOf(
                    arrayOf("Use both uppercase and lowercase letters.", "password"),
                    arrayOf("Use at least one letter.", "password"),
                    arrayOf("Use at least one number.", "password"),
                    arrayOf("Use at least one symbol.", "password"),
                    arrayOf("This password has appeared in a data leak. Choose a different password.", "password"),
                    arrayOf("This email verification link is invalid or has expired.", "email"),
                )
        }
    }
}

private val fill = FieldMessage("Please fill this in.")
