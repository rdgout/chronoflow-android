package net.chronoflow.app.settings

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.WindowManager
import android.view.inspector.WindowInspector
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.chronoflow.app.testing.ScreenTest
import net.chronoflow.core.testing.tiers.Smoke
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@Smoke
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class SettingsSecurityDeviceTest : ScreenTest() {
    @Test fun seededTwoFactorSetupConfirmsAndShowsRecoveryCodes() {
        val started = System.nanoTime()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "settings-security-download.txt")
        file.delete()
        Intents.init()
        try {
            val scenario =
                launch(
                    mapOf(
                        "CF_AUTH_TEST_SIGNED_IN" to "demo",
                        "CF_AUTH_TEST_WALKTHROUGH_SEEN" to "1",
                        "CF_SETTINGS_ROUTE" to "twoFactorSetup",
                        "CF_MOCK_DELAY" to "0",
                    ),
                )
            compose.waitUntilExactlyOneExists(hasTestTag("settings.twoFactor.code"), 15_000)
            compose.onNodeWithTag("settings.twoFactor.secret").assertExists()
            scenario.onActivity {
                assertTrue(it.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
            }
            compose.onNodeWithTag("settings.twoFactor.code").performScrollTo().performTextInput("123456")
            waitForImeToSettle("settings.twoFactor.confirm")
            compose.onNodeWithTag("settings.twoFactor.confirm").performScrollTo().performClick()
            compose.waitUntil(15_000) {
                compose.onAllNodesWithTag("settings.twoFactor.recoveryCode").fetchSemanticsNodes().size == 8
            }
            compose.onAllNodesWithTag("settings.twoFactor.recoveryCode").assertCountEquals(8)
            scenario.onActivity {
                assertTrue(it.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
            }
            Intents.intending(hasAction(Intent.ACTION_CREATE_DOCUMENT)).respondWith(
                ActivityResult(Activity.RESULT_OK, Intent().setData(Uri.fromFile(file))),
            )
            compose.onNodeWithTag("settings.twoFactor.download").performScrollTo().performClick()
            waitFor(5_000) { file.exists() && file.length() > 0 }
            val text = file.readText(Charsets.UTF_8)
            val codes = text.split('\n')
            assertEquals(8, codes.size)
            assertEquals((0..7).map(Int::toString), codes.map { it.substringAfterLast('-') })
            assertTrue(codes.all { Regex("[0-9a-f]{8}-[0-7]").matches(it) })
            assertFalse(text.startsWith('\uFEFF'))
            assertFalse(text.contains('\r'))
            assertFalse(text.endsWith('\n'))
            assertTrue("security setup journey exceeded 10 seconds", System.nanoTime() - started < 10_000_000_000L)
        } finally {
            Intents.release()
            file.delete()
        }
    }

    @Test fun promptKeepsSubmitAboveTheKeyboard() {
        val started = System.nanoTime()
        val scenario =
            launch(
                mapOf(
                    "CF_AUTH_TEST_SIGNED_IN" to "twofactor",
                    "CF_AUTH_TEST_WALKTHROUGH_SEEN" to "1",
                    "CF_SETTINGS_ROUTE" to "twoFactor",
                    "CF_MOCK_DELAY" to "0",
                ),
            )
        compose.waitUntilExactlyOneExists(hasTestTag("settings.twoFactor.disable"), 15_000)
        scenario.onActivity { assertEquals(0, it.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE) }
        compose.onNodeWithTag("settings.twoFactor.disable").performScrollTo().performClick()
        compose.waitUntilExactlyOneExists(hasTestTag("settings.security.prompt.password"), 5_000)
        compose.onNodeWithTag("settings.security.prompt.password").performClick()
        try {
            lateinit var decor: View
            scenario.onActivity { decor = WindowInspector.getGlobalWindowViews().single { it.hasWindowFocus() } }
            waitForImeToSettle("settings.security.prompt.submit", windowDecor = decor)
            var imeTop = 0f
            scenario.onActivity {
                val insets = checkNotNull(ViewCompat.getRootWindowInsets(decor))
                assertTrue(insets.isVisible(WindowInsetsCompat.Type.ime()))
                imeTop = (decor.height - insets.getInsets(WindowInsetsCompat.Type.ime()).bottom).toFloat()
            }
            val submit = compose.onNodeWithTag("settings.security.prompt.submit")
            submit.assertIsDisplayed()
            val node = submit.fetchSemanticsNode()
            assertTrue("prompt submit is above the IME", node.positionInWindow.y + node.size.height <= imeTop)
        } finally {
            compose.onNodeWithTag("settings.security.prompt.cancel").performClick()
            compose.onNodeWithTag("settings.security.prompt.password").assertDoesNotExist()
        }
        assertTrue("security keyboard check exceeded 10 seconds", System.nanoTime() - started < 10_000_000_000L)
    }
}
