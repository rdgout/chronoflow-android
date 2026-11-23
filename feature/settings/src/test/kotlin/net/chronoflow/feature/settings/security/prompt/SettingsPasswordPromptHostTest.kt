package net.chronoflow.feature.settings.security.prompt

import android.view.MotionEvent
import androidx.activity.ComponentDialog
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.settings.security.SettingsSecurityPrompt
import net.chronoflow.feature.settings.support.SettingsScreenTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog
import java.time.Instant

@Logic
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsPasswordPromptHostTest(
    profile: DeviceProfile,
) : SettingsScreenTest(profile) {
    @Test fun busyHostRemainsVisibleOnBack() {
        var dismissed = 0
        val action = SettingsSecurityPrompt.Enable
        val password = TextFieldState()
        show {
            SettingsPasswordPromptHost(true, true, { dismissed++ }) {
                SettingsPasswordPrompt(
                    action.detail,
                    action.button,
                    false,
                    password,
                    true,
                    null,
                    { Instant.parse("2026-11-21T12:00:00Z") },
                    null,
                    null,
                    "rick@example.com",
                    {},
                    {},
                    {},
                )
            }
        }
        val tag = if (profile.widthDp >= 600) "dialog" else "sheet"
        val surface = compose.onNodeWithTag("settings.security.prompt.$tag")
        surface.assertExists().assertWidthIsEqualTo((if (profile.widthDp >= 600) 540 else profile.widthDp).dp)
        if (profile.widthDp >= 600) {
            val bounds = surface.getUnclippedBoundsInRoot()
            assertTrue((bounds.bottom - bounds.top).value <= profile.heightDp * 0.9f)
        }
        compose.runOnIdle {
            (checkNotNull(ShadowDialog.getLatestDialog()) as ComponentDialog).onBackPressedDispatcher.onBackPressed()
        }
        compose.onNodeWithTag("settings.security.prompt.$tag").assertExists()
        assertEquals(0, dismissed)
    }

    @Test fun outsideTouchWhileBusyKeepsTheHost() {
        var dismissed = 0
        val action = SettingsSecurityPrompt.Disable
        val password = TextFieldState()
        show {
            SettingsPasswordPromptHost(true, true, { dismissed++ }) {
                SettingsPasswordPrompt(
                    action.detail,
                    action.button,
                    true,
                    password,
                    true,
                    null,
                    { Instant.parse("2026-11-21T12:00:00Z") },
                    null,
                    null,
                    "rick@example.com",
                    {},
                    {},
                    {},
                )
            }
        }
        compose.runOnIdle {
            val event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_OUTSIDE, -1f, -1f, 0)
            try {
                checkNotNull(ShadowDialog.getLatestDialog()).dispatchTouchEvent(event)
            } finally {
                event.recycle()
            }
        }
        compose.onNodeWithTag("settings.security.prompt").assertExists()
        assertEquals(0, dismissed)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> =
            listOf(
                arrayOf(DeviceProfiles.pixel8),
                arrayOf(DeviceProfiles.foldInner),
                arrayOf(DeviceProfiles.foldInner.copy(name = "foldInnerLargeFont", fontScale = 1.3f)),
            )
    }
}
