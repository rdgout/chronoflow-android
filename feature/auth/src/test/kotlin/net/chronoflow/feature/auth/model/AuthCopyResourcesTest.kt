package net.chronoflow.feature.auth.model

import androidx.test.ext.junit.runners.AndroidJUnit4
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.core.ui.l10n.generated.CopyIndex
import net.chronoflow.core.ui.l10n.generated.CopyTable
import net.chronoflow.feature.auth.shell.resolve
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@Logic
@Config(sdk = [36])
@RunWith(ParameterizedRobolectricTestRunner::class)
class AuthCopyResourcesTest(
    private val language: String,
) {
    @Test fun allBannerAndFieldKeysResolveInBothLanguages() {
        RuntimeEnvironment.setQualifiers(language)
        val resources = RuntimeEnvironment.getApplication().resources
        val texts =
            AuthFormContext.entries.flatMap { context ->
                AuthBannerKind.entries.flatMap { kind ->
                    listOf(kind.title(context), kind.body(context)).filter { it.isNotEmpty() }.map { AuthText(it) }
                }
            } +
                fields.map { AuthText(it) } +
                listOf(
                    AuthText("Use no more than %lld characters.", 255),
                    AuthText("Use at least %lld characters.", 12),
                )
        assertTrue(texts.all { CopyIndex.find(CopyTable.Auth, it.key) != null && it.resolve(resources).isNotEmpty() })
        val form = AuthFormModel()
        form.receive(
            net.chronoflow.core.model.DataFailure
                .Server(null),
        )
        assertEquals(
            if (language ==
                "nl"
            ) {
                "Er ging bij ons iets mis. Probeer het zo meteen opnieuw."
            } else {
                "Something went wrong on our side. Try again in a moment."
            },
            form.takeAnnouncement { it.resolve(resources) },
        )
        assertEquals(
            if (language ==
                "nl"
            ) {
                "Verbinding nodig"
            } else {
                "Needs a connection"
            },
            AuthText("Needs a connection").resolve(resources),
        )
        assertEquals(
            if (language ==
                "nl"
            ) {
                "Gebruik niet meer dan 255 tekens."
            } else {
                "Use no more than 255 characters."
            },
            AuthText("Use no more than %lld characters.", 255).resolve(resources),
        )
        assertEquals(
            if (language ==
                "nl"
            ) {
                "Gebruik hoofdletters en kleine letters."
            } else {
                "Use both uppercase and lowercase letters."
            },
            AuthText("Use both uppercase and lowercase letters.").resolve(resources),
        )
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases() = listOf(arrayOf("en"), arrayOf("nl"))

        private val fields =
            listOf(
                "Please fill this in.",
                "Enter a valid email address, like name@example.com.",
                "The passwords don't match yet.",
                "Use both uppercase and lowercase letters.",
                "Use at least one letter.",
                "Use at least one number.",
                "Use at least one symbol.",
                "This password has appeared in a data leak. Choose a different password.",
                "This email verification link is invalid or has expired.",
                "Enter the 6-digit code from your authenticator app.",
                "Enter one of the recovery codes you saved when you set up two-factor authentication.",
                "The authentication code was not accepted. Try again.",
                "That code didn't work. Codes change every 30 seconds, so try the newest one.",
                "This recovery code was not accepted. Try another saved code.",
            )
    }
}
