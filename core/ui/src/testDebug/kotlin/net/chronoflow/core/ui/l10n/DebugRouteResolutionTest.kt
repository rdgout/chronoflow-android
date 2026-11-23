package net.chronoflow.core.ui.l10n

import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class DebugRouteResolutionTest {
    private fun route(): String =
        RuntimeEnvironment.getApplication().getString(net.chronoflow.core.ui.R.string.app_debug_route_x, "today")

    @Test
    @Config(qualifiers = "en")
    fun debugRouteResolvesInDebug() {
        assertThat(route()).isEqualTo("Debug route: today")
    }

    @Test
    @Config(qualifiers = "nl")
    fun debugRouteResolvesInDutch() {
        assertThat(route()).isEqualTo("Debugroute: today")
    }
}
