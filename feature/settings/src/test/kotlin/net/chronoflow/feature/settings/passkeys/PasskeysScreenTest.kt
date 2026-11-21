package net.chronoflow.feature.settings.passkeys

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.settings.SettingsDebugCounters
import net.chronoflow.feature.settings.security.SecurityState
import net.chronoflow.feature.settings.security.SettingsSecurityPrompt
import net.chronoflow.feature.settings.support.settingsNow
import net.chronoflow.feature.settings.testing.FakeSecretField
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PasskeysScreenTest : PasskeysTestHost() {
    private val server = PasskeyServer()

    private fun state(security: SecurityState = SecurityState()) =
        PasskeysScreenState(
            security = security,
            inputs = PasskeysInputs(),
            now = { settingsNow },
        )

    @Test fun loadingShowsSkeletonRows() {
        show { PasskeysContent(state(), {}) }
        compose.onNodeWithTag("settings.loading").assertExists()
    }

    @Test fun listShowsNameMetaAndARemoveButtonPerRow() {
        show { PasskeysContent(state(SecurityState(passkeys = server.passkeys)), {}) }
        compose.onNodeWithTag("settings.passkeys.list").assertExists()
        compose.onAllNodesWithTag("settings.passkeys.remove").assertCountEquals(2)
        compose.onAllNodesWithTag("settings.passkeys.remove").onFirst().assertHasClickAction()
    }

    @Test fun emptyShowsTheDashedCard() {
        show { PasskeysContent(state(SecurityState(passkeys = emptyList())), {}) }
        compose.onNodeWithTag("settings.passkeys.empty").assertExists()
    }

    @Test fun unavailableShowsRetryAndRetryReloads() {
        var retries = 0
        show {
            PasskeysContent(state(SecurityState(passkeysUnavailable = true)), {
                if (it ==
                    PasskeysEvent.Refresh
                ) {
                    retries++
                }
            })
        }
        compose.onNodeWithTag("settings.passkeys.retry").performClick()
        assertEquals(1, retries)
    }

    @Test fun offlineDisablesAddAndShowsTheLine() {
        show { PasskeysContent(state(SecurityState(offline = true, passkeysUnavailable = true)), {}) }
        compose.onNodeWithTag("settings.passkeys.add").assertIsNotEnabled()
        compose.onNodeWithTag("settings.passkeys.offline").assertExists()
    }

    @Test fun removeOpensThePromptWithTheNameInTheDetail() {
        deps.security = server::perform
        val model = route()
        compose.onAllNodesWithTag("settings.passkeys.remove").onFirst().performClick()
        scheduler.runCurrent()
        compose.onNodeWithTag("settings.passkeys.prompt").assertExists()
        assertEquals(SettingsSecurityPrompt.RemovePasskey, model.ui.value.security.prompt)
        assertEquals(
            "iPhone 16",
            model.ui.value.security.removal
                ?.name,
        )
    }

    @Test fun addOpensThePromptWithTheAddDetail() {
        deps.security = server::perform
        val model = route()
        compose.onNodeWithTag("settings.passkeys.add").performScrollTo().performClick()
        scheduler.runCurrent()
        compose.onNodeWithTag("settings.passkeys.prompt").assertExists()
        assertEquals(SettingsSecurityPrompt.AddPasskey, model.ui.value.security.prompt)
    }

    @Test fun successMessageIsPresentedOnceAfterThePromptCloses() {
        deps.security = server::perform
        val model = route()
        SettingsDebugCounters.securityFeedback.set(0)
        model.event(PasskeysEvent.Add)
        scheduler.runCurrent()
        model.event(PasskeysEvent.Submit(FakeSecretField("pw")))
        scheduler.runCurrent()
        compose.waitUntil(1000) { SettingsDebugCounters.securityFeedback.get() == 1 }
        scheduler.runCurrent()
        compose.onNodeWithTag("settings.passkeys.prompt").assertDoesNotExist()
        assertEquals(null, model.ui.value.feedback)
    }
}
