package net.chronoflow.feature.auth.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthNavigationTests {
    @Test fun blankAndWhitespaceChallengesCancelTwoFactor() {
        val navigation = AuthNavigation()
        assertTrue(navigation.twoFactor("artificial-challenge"))
        assertFalse(navigation.twoFactor(""))
        assertNull(navigation.challengeID)
        assertTrue(navigation.twoFactor("artificial-challenge"))
        assertFalse(navigation.twoFactor(" \t\n"))
        assertNull(navigation.challengeID)
    }

    @Test fun challengeStaysInMemoryUntilDiscardCancelOrReset() {
        val navigation = AuthNavigation()
        assertTrue(navigation.twoFactor("artificial-challenge"))
        assertEquals("artificial-challenge", navigation.challengeID)
        assertFalse(navigation.toString().contains("artificial-challenge"))
        navigation.discardChallenge()
        assertNull(navigation.challengeID)
        navigation.twoFactor("artificial-challenge")
        navigation.cancelTwoFactor()
        assertNull(navigation.challengeID)
        navigation.twoFactor("artificial-challenge")
        navigation.reset()
        assertNull(navigation.challengeID)
    }
}
