package net.chronoflow.feature.auth.model

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class AuthBannerKindTests(
    private val kind: AuthBannerKind,
    private val context: AuthFormContext,
    private val title: String,
    private val body: String,
) {
    @Test fun bannerCopyMatchesEveryContext() {
        assertEquals(title, kind.title(context))
        assertEquals(body, kind.body(context))
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}-{1}")
        fun cases(): List<Array<Any>> =
            AuthFormContext.entries.flatMap { context ->
                listOf(
                    arrayOf<Any>(
                        AuthBannerKind.Credentials,
                        context,
                        "That email and password don't match.",
                        "Check both and try again, or reset your password.",
                    ),
                    arrayOf<Any>(
                        AuthBannerKind.EmailTaken,
                        context,
                        "There's already an account with this email.",
                        "Sign in instead, or reset your password.",
                    ),
                    arrayOf<Any>(
                        AuthBannerKind.Offline,
                        context,
                        "Needs a connection",
                        "",
                    ),
                    arrayOf<Any>(
                        AuthBannerKind.Locked,
                        context,
                        if (context ==
                            AuthFormContext.Forgot
                        ) {
                            "Too many requests."
                        } else {
                            "Too many attempts."
                        },
                        when (context) {
                            AuthFormContext.SignIn -> SIGN_IN_LOCKED_BODY
                            AuthFormContext.Forgot -> "Wait a few minutes before you ask for another link."
                            else -> "Wait a few minutes before you try again."
                        },
                    ),
                    arrayOf<Any>(
                        AuthBannerKind.Server,
                        context,
                        "Something went wrong on our side.",
                        when (context) {
                            AuthFormContext.SignUp -> "Your details are kept. Try again in a moment."
                            AuthFormContext.Reset -> "Your new password wasn't saved. Try again in a moment."
                            AuthFormContext.Verify -> "We couldn't send the link. Try again in a moment."
                            else -> "Try again in a moment."
                        },
                    ),
                )
            }
    }
}

private const val SIGN_IN_LOCKED_BODY = "Wait a few minutes before you try again, or reset your password now."
