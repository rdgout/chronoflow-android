package net.chronoflow.app

import android.content.Context
import android.util.TypedValue
import androidx.test.core.app.ApplicationProvider
import net.chronoflow.core.testing.tiers.Layout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@Layout
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29, 31, 36])
class SplashThemeTest {
    @Test fun lightSplashUsesLaunchResources() = verify(false)

    @Test fun nightSplashUsesLaunchResources() = verify(true)

    private fun verify(night: Boolean) {
        RuntimeEnvironment.setQualifiers(if (night) "+night" else "+notnight")
        val context = ApplicationProvider.getApplicationContext<Context>()
        val theme = context.resources.newTheme().apply { applyStyle(R.style.Theme_ChronoFlow_Starting, true) }

        fun resolve(attribute: Int): TypedValue =
            TypedValue().also { assertTrue(theme.resolveAttribute(attribute, it, true)) }
        val background = resolve(androidx.core.splashscreen.R.attr.windowSplashScreenBackground)
        assertEquals(context.getColor(R.color.launch_sky_top), context.getColor(background.resourceId))
        assertEquals(
            R.drawable.launch_moon,
            resolve(androidx.core.splashscreen.R.attr.windowSplashScreenAnimatedIcon).resourceId,
        )
        val post = resolve(androidx.core.splashscreen.R.attr.postSplashScreenTheme).resourceId
        val postTheme = context.resources.newTheme().apply { applyStyle(post, true) }
        val window = TypedValue()
        assertTrue(postTheme.resolveAttribute(android.R.attr.windowBackground, window, true))
        assertEquals(context.getColor(R.color.launch_sky_top), context.getColor(window.resourceId))
        if (android.os.Build.VERSION.SDK_INT >=
            33
        ) {
            assertEquals(1, resolve(android.R.attr.windowSplashScreenBehavior).data)
        }
    }
}
