package net.chronoflow.app

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import net.chronoflow.core.testing.tiers.Smoke
import org.junit.Rule
import org.junit.Test

@Smoke
class AppLaunchSmokeTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun launchesAsNativeDebugUnderStrictMode() {
        compose.waitUntil(5_000) { !(compose.activity.application as ChronoFlowApp).launchPath.isActive }
        assertThat(compose.activity.packageName).isEqualTo("net.chronoflow.app.debug")
        assertThat(
            compose.activity.lifecycle.currentState
                .isAtLeast(Lifecycle.State.RESUMED),
        ).isTrue()
    }
}
