package net.chronoflow.feature.auth.signup

import android.content.ActivityNotFoundException
import android.content.Intent
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.auth.model.AuthPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PolicyOpenerTest {
    @Test fun startsThePolicyInACustomTab() {
        val intents = mutableListOf<Intent>()
        CustomTabPolicyOpener(RuntimeEnvironment.getApplication(), false, intents::add).open(AuthPolicy.Terms.url)
        val intent = intents.single()
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(AuthPolicy.Terms.url, intent.dataString)
        assertTrue(intent.hasExtra("android.support.customtabs.extra.SESSION"))
    }

    @Test fun missingCustomTabFallsBackToView() {
        val intents = mutableListOf<Intent>()
        CustomTabPolicyOpener(RuntimeEnvironment.getApplication(), true) {
            intents += it
            if (intents.size == 1) throw ActivityNotFoundException()
        }.open(AuthPolicy.Privacy.url)
        assertEquals(2, intents.size)
        assertEquals(Intent.ACTION_VIEW, intents.last().action)
        assertEquals(AuthPolicy.Privacy.url, intents.last().dataString)
        assertFalse(intents.last().hasExtra("android.support.customtabs.extra.SESSION"))
    }

    @Test fun noHandlerLeavesTheScreenAlone() {
        var attempts = 0
        CustomTabPolicyOpener(RuntimeEnvironment.getApplication(), false) {
            attempts++
            throw ActivityNotFoundException()
        }.open(AuthPolicy.Terms.url)
        assertEquals(2, attempts)
    }

    @Test fun unknownUrlIsIgnored() {
        var attempts = 0
        CustomTabPolicyOpener(RuntimeEnvironment.getApplication(), false) { attempts++ }.open("https://example.com")
        assertEquals(0, attempts)
    }
}
