package net.chronoflow.debug

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class OnboardingSeedFlagsTest(
    private val key: String?,
    private val offline: Boolean,
) {
    @Test fun mutationsOfflineFollowsPendingSeeds() {
        val seed =
            TestSeed.parse(
                mapOf(TestSeed.TEST_ID_KEY to "019a562b-8000-7000-8000-000000000001") +
                    (key?.let { mapOf(it to "1") } ?: emptyMap()),
            )
        assertEquals(offline, seed.mutationsOffline)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}-offline={1}")
        fun flags() =
            listOf(
                arrayOf<Any?>("CF_WALKTHROUGH_TEST_PENDING", true),
                arrayOf<Any?>("CF_AUTH_TEST_TERMS_PENDING", true),
                arrayOf<Any?>("CF_AUTH_TEST_TERMS_CHANGES_PENDING", false),
                arrayOf<Any?>(null, false),
            )
    }
}
