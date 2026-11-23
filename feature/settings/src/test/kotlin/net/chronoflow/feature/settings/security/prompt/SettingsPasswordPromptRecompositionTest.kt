@file:OptIn(androidx.compose.runtime.InternalComposeTracingApi::class)

package net.chronoflow.feature.settings.security.prompt

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composer
import androidx.compose.runtime.CompositionTracer
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.settings.security.SettingsSecurityPrompt
import net.chronoflow.feature.settings.support.SettingsScreenTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsPasswordPromptRecompositionTest : SettingsScreenTest() {
    @Test fun countdownRecomposesOnlyTheLockoutTextUntilUnlock() {
        var now = Instant.parse("2026-11-21T12:00:00Z")
        val retryAt = now.plusSeconds(3)
        val password = TextFieldState("password")
        var prompt = 0
        var submit = 0
        var lockout = 0
        val tracer =
            object : CompositionTracer {
                override fun isTraceInProgress() = true

                override fun traceEventStart(
                    key: Int,
                    dirty1: Int,
                    dirty2: Int,
                    info: String,
                ) {
                    if (".SettingsPasswordPrompt (" in info) prompt++
                    if (".CfButton (" in info) submit++
                    if (".PromptLockoutText (" in info) lockout++
                }

                override fun traceEventEnd() = Unit
            }
        Composer.setTracer(tracer)
        try {
            show {
                SettingsPasswordPrompt(
                    SettingsSecurityPrompt.Enable.detail,
                    SettingsSecurityPrompt.Enable.button,
                    false,
                    password,
                    false,
                    retryAt,
                    { now },
                    null,
                    null,
                    "rick@example.com",
                    {},
                    {},
                    {},
                )
            }
            compose.runOnIdle {
                assertTrue("Every measured scope must have composed", prompt > 0 && submit > 0 && lockout > 0)
                prompt = 0
                submit = 0
                lockout = 0
                now = now.plusSeconds(1)
            }
            compose.mainClock.advanceTimeBy(1001)
            compose.onNodeWithTag("settings.security.prompt.submit").assertIsNotEnabled()
            compose.runOnIdle {
                assertTrue("The countdown leaf must update", lockout > 0)
                assertEquals(0, prompt)
                assertEquals(0, submit)
                now = now.plusSeconds(2)
            }
            compose.mainClock.advanceTimeBy(1001)
            compose.onNodeWithTag("settings.security.prompt.submit").assertIsEnabled()
            compose.onNodeWithTag("settings.security.prompt.error").assertDoesNotExist()
            compose.runOnIdle { assertTrue("Unlock must update the controls", prompt > 0 && submit > 0) }
        } finally {
            Composer.setTracer(null)
        }
    }
}
