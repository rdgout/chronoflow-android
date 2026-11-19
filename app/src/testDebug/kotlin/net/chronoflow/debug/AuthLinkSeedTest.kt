package net.chronoflow.debug

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthLinkSeedTest {
    private val id = "0199a3c0-0000-7000-8000-000000000001"
    private val link = "https://app.chronoflow.net/reset-password/seed-secret?email=a@b.c"

    @Test fun gatedHttpsLinkIsCapturedWithoutPrintingItsSecret() {
        val seed = TestSeed.parse(mapOf(TestSeed.TEST_ID_KEY to id, "CF_AUTH_TEST_LINK" to link))
        assertEquals(link, seed.authTestLink)
        assertFalse(seed.toString().contains("seed-secret"))
    }

    @Test fun ungatedLinkIsIgnored() {
        val seed = TestSeed.parse(mapOf("CF_AUTH_TEST_LINK" to link))
        assertNull(seed.authTestLink)
        assertTrue("CF_AUTH_TEST_LINK" in seed.ignoredKeys)
    }

    @Test fun nonHttpsLinkIsIgnored() {
        val seed =
            TestSeed.parse(
                mapOf(
                    TestSeed.TEST_ID_KEY to id,
                    "CF_AUTH_TEST_LINK" to "http://app.chronoflow.net/reset-password/t",
                ),
            )
        assertNull(seed.authTestLink)
        assertTrue("CF_AUTH_TEST_LINK" in seed.ignoredKeys)
    }
}
