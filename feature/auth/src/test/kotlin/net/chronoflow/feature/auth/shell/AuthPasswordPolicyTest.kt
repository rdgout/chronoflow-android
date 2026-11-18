package net.chronoflow.feature.auth.shell

import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.core.ui.l10n.generated.CopyIndex
import net.chronoflow.core.ui.l10n.generated.CopyTable
import net.chronoflow.feature.auth.form.AuthPasswordPolicy
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@Logic
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36])
class AuthPasswordPolicyTest(
    private val language: String,
    private val expected: String,
) {
    @Test fun hintResolvesFromTheCatalog() {
        RuntimeEnvironment.setQualifiers(language)
        val resource = checkNotNull(CopyIndex.find(CopyTable.Auth, AuthPasswordPolicy.HINT_KEY))
        assertEquals(expected, RuntimeEnvironment.getApplication().resources.getString(resource.resId))
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases() =
            listOf(
                arrayOf(
                    "en",
                    "Use at least 12 characters, with uppercase and lowercase letters, numbers and symbols. Choose a password that has not appeared in a data leak.",
                ),
                arrayOf(
                    "nl",
                    "Gebruik minstens 12 tekens, met hoofdletters, kleine letters, cijfers en symbolen. Kies een wachtwoord dat niet in een datalek voorkomt.",
                ),
            )
    }
}
