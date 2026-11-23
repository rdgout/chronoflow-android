package net.chronoflow.feature.launch.welcome

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.feature.launch.model.LaunchIntro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.time.Duration.Companion.milliseconds

@Layout
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class WelcomeRecompositionTest {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)

    @Test fun liveIntroComposesOnceAndStopsAfterTheLastItem() {
        compose.mainClock.autoAdvance = false
        val clock = CountingWelcomeClock().apply { advance(2200.milliseconds) }
        val intro = LaunchIntro(2300.milliseconds, clock)
        val probe = WelcomeProbe()
        val live = mutableStateOf(true)
        compose.setContent {
            ProfileHost(DeviceProfiles.pixel8) {
                ChronoFlowTheme(darkTheme = false) {
                    CompositionLocalProvider(LocalWelcomeProbe provides probe) {
                        WelcomeScreen(if (live.value) intro else null, null, {}, {})
                    }
                }
            }
        }
        compose.waitForIdle()
        assertTrue(intro.hasPlayed)
        var readsAfterLanding = 0
        for (ms in 2208..3400 step 8) {
            compose.runOnIdle { clock.advance(ms.milliseconds) }
            compose.mainClock.advanceTimeBy(8, ignoreFrameDuration = true)
            compose.waitForIdle()
            if (ms == 3248) readsAfterLanding = clock.reads
        }
        assertEquals(1, probe.compositions)
        assertEquals(readsAfterLanding, clock.reads)
        assertEquals(0, clock.pendingSleeps)
        val landed = welcomeTags.map { compose.bounds(it) }
        compose.runOnIdle { live.value = false }
        compose.mainClock.advanceTimeByFrame()
        assertEquals(landed, welcomeTags.map { compose.bounds(it) })
    }
}
