package net.chronoflow.core.ui.links

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.browser.customtabs.CustomTabsIntent
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PolicyLinksTest {
    @Test fun opensTermsInACustomTab() =
        withActivity { activity ->
            val intents = mutableListOf<Intent>()
            PolicyLinks.open(activity, PolicyLinks.TERMS, false, intents::add)
            val intent = intents.single()
            assertEquals(Intent.ACTION_VIEW, intent.action)
            assertEquals("https://chronoflow.net/terms", intent.dataString)
            assertTrue(intent.hasExtra(CustomTabsIntent.EXTRA_SESSION))
        }

    @Test fun colorSchemeFollowsTheTheme() =
        withActivity { activity ->
            val intents = mutableListOf<Intent>()
            PolicyLinks.open(activity, PolicyLinks.TERMS, true, intents::add)
            PolicyLinks.open(activity, PolicyLinks.TERMS, false, intents::add)
            assertEquals(
                CustomTabsIntent.COLOR_SCHEME_DARK,
                intents[0].getIntExtra(CustomTabsIntent.EXTRA_COLOR_SCHEME, -1),
            )
            assertEquals(
                CustomTabsIntent.COLOR_SCHEME_LIGHT,
                intents[1].getIntExtra(CustomTabsIntent.EXTRA_COLOR_SCHEME, -1),
            )
        }

    @Test fun fallsBackToAPlainViewIntent() =
        withActivity { activity ->
            val intents = mutableListOf<Intent>()
            PolicyLinks.open(activity, PolicyLinks.TERMS, false) {
                intents += it
                if (intents.size == 1) throw ActivityNotFoundException()
            }
            assertEquals(2, intents.size)
            assertEquals(Intent.ACTION_VIEW, intents.last().action)
            assertEquals(PolicyLinks.TERMS, intents.last().dataString)
            assertFalse(intents.last().hasExtra(CustomTabsIntent.EXTRA_SESSION))
        }

    @Test fun fallsBackWithoutCrashingWhenNothingHandlesIt() =
        withActivity { activity ->
            var attempts = 0
            PolicyLinks.open(activity, PolicyLinks.TERMS, false) {
                attempts++
                throw ActivityNotFoundException()
            }
            assertEquals(2, attempts)
        }

    private fun withActivity(block: (ComponentActivity) -> Unit) {
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        try {
            block(controller.get())
        } finally {
            controller.pause().stop().destroy()
        }
    }
}
