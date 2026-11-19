package net.chronoflow.app.links

import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import net.chronoflow.app.MainActivity
import net.chronoflow.app.SessionTestApplication
import net.chronoflow.core.testing.AuthLinkUrls
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config

@Logic
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36], application = SessionTestApplication::class)
class AppLinkIntentFilterTest(
    private val name: String,
    private val url: String,
    private val resolves: Boolean,
) {
    @Test fun onlyTheDeclaredHttpsAuthPrefixesResolveToMainActivity() {
        val context = ApplicationProvider.getApplicationContext<SessionTestApplication>()
        val intent =
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(url),
            ).addCategory(Intent.CATEGORY_BROWSABLE).setPackage(context.packageName)

        @Suppress("DEPRECATION")
        val actual = context.packageManager.queryIntentActivities(intent, 0).map { it.activityInfo.name }
        assertEquals(if (resolves) listOf(MainActivity::class.java.name) else emptyList<String>(), actual)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> =
            listOf(
                arrayOf("reset", "https://app.chronoflow.net/reset-password/t?email=a%40b.c", true),
                arrayOf(
                    "verify",
                    AuthLinkUrls.verify("0199a3c0-0000-7000-8000-000000000001", "a".repeat(40), 1, "b".repeat(64)),
                    true,
                ),
                arrayOf("http", "http://app.chronoflow.net/reset-password/t", false),
                arrayOf("host", "https://evil.test/reset-password/t", false),
                arrayOf("path", "https://app.chronoflow.net/other", false),
                arrayOf("no-slash", "https://app.chronoflow.net/reset-password", false),
            )
    }
}
