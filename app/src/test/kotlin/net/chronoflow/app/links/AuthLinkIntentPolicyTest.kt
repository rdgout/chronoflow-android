package net.chronoflow.app.links

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class AuthLinkIntentPolicyTest(
    private val name: String,
    private val action: String?,
    private val data: String?,
    private val flags: Int,
    private val restored: Boolean,
    private val expected: String?,
) {
    @Test fun capturesOnlyFreshHttpsViewIntents() {
        assertEquals(expected, AuthLinkIntentPolicy.linkToCapture(action, data, flags, restored))
    }

    companion object {
        private const val VIEW = "android.intent.action.VIEW"
        private const val LINK = "https://app.chronoflow.net/reset-password/t?email=a@b.c"

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<Array<Any?>> =
            listOf(
                arrayOf("restored", VIEW, LINK, 0, true, null),
                arrayOf("history", VIEW, LINK, 0x00100000, false, null),
                arrayOf("main", "android.intent.action.MAIN", LINK, 0, false, null),
                arrayOf("http", VIEW, "http://app.chronoflow.net/reset-password/t?email=a@b.c", 0, false, null),
                arrayOf("view", VIEW, LINK, 0, false, LINK),
                arrayOf("null-data", VIEW, null, 0, false, null),
            )
    }
}
