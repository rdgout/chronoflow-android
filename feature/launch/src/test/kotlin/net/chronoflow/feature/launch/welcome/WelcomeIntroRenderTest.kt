package net.chronoflow.feature.launch.welcome

import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.feature.launch.model.LaunchIntro
import net.chronoflow.feature.launch.model.ManualLaunchClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Layout
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class WelcomeIntroRenderTest {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)

    @Test fun frozenIntroPositionsFollowTheIntroCurve() {
        val active = mutableStateOf<LaunchIntro?>(null)
        compose.setContent {
            ProfileHost(DeviceProfiles.pixel8) {
                ChronoFlowTheme(darkTheme = false) { key(active.value) { WelcomeScreen(active.value, null, {}, {}) } }
            }
        }
        val tops = welcomeTags.map { compose.bounds(it).top.value }
        for (ms in listOf(2299, 2400, 2450, 2700, 3200, 3300)) {
            val intro = LaunchIntro(2300.milliseconds, ManualLaunchClock(), ms.milliseconds)
            compose.runOnIdle { active.value = intro }
            compose.waitForIdle()
            assertTrue("new frozen intro composed at $ms", intro.hasPlayed)
            for ((index, tag) in welcomeTags.withIndex()) {
                val expected = tops[index] + 14 * (1 - intro.progress(index, ms.milliseconds)).toFloat()
                assertEquals(
                    "$tag at $ms",
                    expected,
                    compose
                        .onNodeWithTag(tag, useUnmergedTree = true)
                        .fetchSemanticsNode()
                        .boundsInRoot.top /
                        DeviceProfiles.pixel8.density,
                    0.5f / DeviceProfiles.pixel8.density,
                )
            }
        }
    }

    @Test fun frozenIntroPixelsStayFixedAcrossClockAndRenderPasses() {
        compose.mainClock.autoAdvance = false
        val clock = CountingWelcomeClock()
        val intro = LaunchIntro(2300.milliseconds, clock, 2700.milliseconds)
        compose.setContent {
            ProfileHost(
                DeviceProfiles.pixel8,
            ) { ChronoFlowTheme(darkTheme = false) { WelcomeScreen(intro, null, {}, {}) } }
        }
        val first = compose.onRoot().captureToImage().asAndroidBitmap()
        clock.advance(10.seconds)
        compose.mainClock.advanceTimeBy(1000)
        val second = compose.onRoot().captureToImage().asAndroidBitmap()
        assertTrue(first.sameAs(second))
        assertEquals(0, clock.reads)
        assertEquals(0, clock.pendingSleeps)
    }

    @Test fun laterAppearanceAndReduceMotionShowTheFinalScreen() {
        val active = mutableStateOf<LaunchIntro?>(null)
        val reduce = mutableStateOf(false)
        compose.setContent {
            ProfileHost(DeviceProfiles.pixel8) {
                ChronoFlowTheme(darkTheme = false, reduceMotion = reduce.value) {
                    key(active.value) { WelcomeScreen(active.value, null, {}, {}) }
                }
            }
        }
        val final = compose.onRoot().captureToImage().asAndroidBitmap()
        val claimed = LaunchIntro(2300.milliseconds, ManualLaunchClock()).apply { claim() }
        compose.runOnIdle { active.value = claimed }
        assertTrue(final.sameAs(compose.onRoot().captureToImage().asAndroidBitmap()))
        val untouched = LaunchIntro(2300.milliseconds, ManualLaunchClock())
        compose.runOnIdle {
            reduce.value = true
            active.value = untouched
        }
        assertTrue(final.sameAs(compose.onRoot().captureToImage().asAndroidBitmap()))
        assertFalse(untouched.hasPlayed)
    }
}
