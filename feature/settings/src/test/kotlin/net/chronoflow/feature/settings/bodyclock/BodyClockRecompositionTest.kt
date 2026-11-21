@file:OptIn(androidx.compose.runtime.InternalComposeTracingApi::class)

package net.chronoflow.feature.settings.bodyclock

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composer
import androidx.compose.runtime.CompositionTracer
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.settings.support.SettingsScreenTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BodyClockRecompositionTest :
    SettingsScreenTest(
        DeviceProfiles.pixel8.copy(name = "allBodyClockSections", heightDp = 6000, density = 1f),
    ) {
    @Test fun chronotypeTapLeavesCaffeineAndLightUnchanged() {
        var about = 0
        var caffeine = 0
        var light = 0
        var parent = 0
        val tracer =
            object : CompositionTracer {
                override fun isTraceInProgress() = true

                override fun traceEventStart(
                    key: Int,
                    dirty1: Int,
                    dirty2: Int,
                    info: String,
                ) {
                    if (".BodyClockAbout (" in info) about++
                    if (".BodyClockCaffeine (" in info) caffeine++
                    if (".BodyClockLight (" in info) light++
                }

                override fun traceEventEnd() = Unit
            }
        val selected = mutableStateOf("early")
        val field = TextFieldState()
        val event: (BodyClockEvent) -> Unit = { if (it is BodyClockEvent.Choice) selected.value = it.id }
        Composer.setTracer(tracer)
        try {
            show {
                SideEffect { parent++ }
                BodyClockScreen(
                    BodyClockScreenState(bodyClockUi("""{"chronotype":"${selected.value}"}"""), field),
                    event,
                )
            }
            compose.runOnIdle {
                assertTrue(
                    "Every tested section must be composed before the tap",
                    about > 0 && caffeine > 0 && light > 0,
                )
                about = 0
                caffeine = 0
                light = 0
            }
            compose.onNodeWithTag("settings.bodyClock.chronotype.late").performClick()
            compose.runOnIdle {
                assertTrue(parent > 1)
                assertTrue(about > 0)
                assertEquals(0, caffeine)
                assertEquals(0, light)
            }
        } finally {
            Composer.setTracer(null)
        }
    }
}
