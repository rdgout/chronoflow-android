package net.chronoflow.feature.auth.shell

import androidx.compose.ui.test.junit4.v2.createComposeRule
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.auth.form.AuthPasswordPolicy
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config

@Logic
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36])
class AuthPasswordPolicyTest(
    private val language: String,
    private val expected: String,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain =
        RuleChain
            .outerRule(
                ProfileRule(DeviceProfiles.pixel8, if (language == "nl") AppLanguage.Nl else AppLanguage.En),
            ).around(compose)

    @Test fun hintResolvesFromTheCatalog() {
        var actual: String? = null
        compose.setContent { actual = AuthPasswordPolicy.hint() }
        compose.runOnIdle { assertEquals(expected, actual) }
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
