package net.chronoflow.feature.settings.profile

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.onNodeWithTag
import net.chronoflow.core.model.store.AccountRecord
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.settings.SettingsPagesState
import net.chronoflow.feature.settings.account.AccountScreen
import net.chronoflow.feature.settings.account.AccountScreenState
import net.chronoflow.feature.settings.support.SettingsScreenTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AccountProfileScreenTest : SettingsScreenTest() {
    private val account = AccountRecord("account", "Rick Gout", "rick@example.com")

    @Test fun accountHasProfileAndLanguageRows() {
        show { AccountScreen(AccountScreenState(SettingsPagesState(account = account, loaded = true)), {}) }
        compose.onNodeWithTag("settings.account.profile").assertExists()
        compose.onNodeWithTag("settings.account.language").assertExists()
        compose.onNodeWithTag("settings.account.delete").assertDoesNotExist()
    }

    @Test fun emailHasNoEditingAction() {
        val ui =
            ProfileUiState(SettingsPagesState(account = account), SettingsProfileState(account.name, account.email))
        show { ProfileScreen(ProfileScreenState(ui, TextFieldState(account.name)), {}) }
        compose.onNodeWithTag("settings.profile.email").assert(SemanticsMatcher.keyNotDefined(SemanticsActions.SetText))
    }

    @Test fun savedHintAppearsAfterSave() {
        val ui =
            ProfileUiState(
                SettingsPagesState(account = account),
                SettingsProfileState(account.name, account.email, saved = true),
            )
        show { ProfileScreen(ProfileScreenState(ui, TextFieldState(account.name)), {}) }
        compose.onNodeWithTag("settings.profile.saved").assertExists()
    }
}
