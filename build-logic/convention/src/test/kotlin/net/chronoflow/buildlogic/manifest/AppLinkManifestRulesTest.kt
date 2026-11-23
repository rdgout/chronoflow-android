package net.chronoflow.buildlogic.manifest

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class AppLinkManifestRulesTest(
    private val mutation: String,
) {
    @Test fun rejectsMissingUnverifiedOrBroadenedAuthLinks() {
        val valid = checkNotNull(javaClass.getResource("/manifests/valid.xml")).readText()
        val start = valid.indexOf("<intent-filter android:autoVerify=\"true\">")
        val end = valid.indexOf("</intent-filter>", start) + "</intent-filter>".length
        val filter = valid.substring(start, end)
        val broken =
            when (mutation) {
                "missing" -> {
                    valid.replace(filter, "")
                }

                "not-verified" -> {
                    valid.replace("android:autoVerify=\"true\"", "android:autoVerify=\"false\"")
                }

                "scheme" -> {
                    valid.replace("android:scheme=\"https\"", "android:scheme=\"http\"")
                }

                "host" -> {
                    valid.replace("android:host=\"app.chronoflow.net\"", "android:host=\"evil.test\"")
                }

                "path" -> {
                    valid.replace("android:pathPrefix=\"/reset-password/\"", "android:pathPrefix=\"/\"")
                }

                "port" -> {
                    valid.replace(
                        "android:host=\"app.chronoflow.net\"",
                        "android:host=\"app.chronoflow.net\" android:port=\"443\"",
                    )
                }

                "duplicate" -> {
                    valid.replace(filter, filter + filter)
                }

                "category" -> {
                    valid.replace("<category android:name=\"android.intent.category.BROWSABLE\" />", "")
                }

                "pattern" -> {
                    valid.replace("android:pathPrefix=\"/reset-password/\"", "android:pathPattern=\".*\"")
                }

                else -> {
                    error("unknown mutation")
                }
            }
        assertTrue(
            ManifestRules.check(broken, "net.chronoflow.app", emptySet()).any { it.rule == Rule.APP_LINK },
        )
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases() =
            listOf("missing", "not-verified", "scheme", "host", "path", "port", "duplicate", "category", "pattern")
    }
}
