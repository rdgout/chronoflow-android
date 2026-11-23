@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.feature.onboarding.walkthrough

import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.SafeInsetsDp
import net.chronoflow.feature.onboarding.OnboardingInsets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class WalkthroughGeometryTests(
    private val profile: DeviceProfile,
    private val customInsets: Boolean,
) {
    @Test fun deviceTableUsesEveryInsetAndKeepsRoomForFixedControls() {
        val i = if (customInsets) SafeInsetsDp(19f, 7f, 31f, 11f) else profile.insets
        val g =
            WalkthroughGeometry(
                profile.widthDp.toFloat(),
                profile.heightDp.toFloat(),
                OnboardingInsets(i.left, i.top, i.right, i.bottom),
            )
        assertEquals(i.top + 8, g.contentTop, .001f)
        assertEquals(profile.heightDp - maxOf(i.bottom, 32f), g.contentTop + g.contentHeight, .001f)
        val available = profile.widthDp - i.left - i.right - 32
        val expected = if (profile.widthDp >= 600) minOf(available, 560f) else available
        assertEquals(expected, g.contentWidth, .001f)
        assertEquals(i.left + 16 + (available - expected) / 2, g.contentLeft, .001f)
        assertTrue(g.contentHeight > 48 + 8 + 20 + 44)
    }

    @Test fun wideWindowCentresA560Column() {
        val g = WalkthroughGeometry(914f, 411f, OnboardingInsets(0f, 0f, 48f, 0f))
        assertEquals(560f, g.contentWidth, .001f)
        assertEquals(g.contentLeft - g.leading, 914f - g.trailing - g.contentLeft - g.contentWidth, .001f)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}-customInsets={1}")
        fun profiles() = DeviceProfiles.all.flatMap { p -> listOf(false, true).map { arrayOf<Any>(p, it) } }
    }
}
