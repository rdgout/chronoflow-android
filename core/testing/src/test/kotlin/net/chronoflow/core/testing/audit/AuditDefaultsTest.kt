package net.chronoflow.core.testing.audit

import org.junit.Assert.assertEquals
import org.junit.Test

class AuditDefaultsTest {
    private val seeds = mapOf("CF_AUTH_TEST_SIGNED_IN" to "demo")

    @Test fun noArgumentsLeaveSeedsUnchanged() {
        assertEquals(seeds, mergeAuditDefaults(seeds, emptyMap()))
    }

    @Test fun languageArgumentAddsTheAppLanguage() {
        assertEquals(
            seeds + ("cf.settings.language" to "nl"),
            mergeAuditDefaults(
                seeds,
                mapOf("cf.audit.language" to "nl"),
            ),
        )
    }

    @Test fun themeArgumentAddsTheAppTheme() {
        assertEquals(
            seeds + ("cf.settings.theme" to "dark"),
            mergeAuditDefaults(seeds, mapOf("cf.audit.theme" to "dark")),
        )
    }

    @Test fun reduceMotionArgumentAddsTheAppPreference() {
        assertEquals(
            seeds + ("cf.reduceMotion" to "1"),
            mergeAuditDefaults(seeds, mapOf("cf.audit.reduceMotion" to "1")),
        )
    }

    @Test fun explicitTestValuesWinOverEveryAuditDefault() {
        val explicit =
            seeds +
                mapOf(
                    "cf.settings.language" to "en",
                    "cf.settings.theme" to "light",
                    "cf.reduceMotion" to "0",
                )
        assertEquals(
            explicit,
            mergeAuditDefaults(
                explicit,
                mapOf(
                    "cf.audit.language" to "nl",
                    "cf.audit.theme" to "dark",
                    "cf.audit.reduceMotion" to "1",
                ),
            ),
        )
    }

    @Test fun unknownArgumentsAreIgnored() {
        assertEquals(seeds, mergeAuditDefaults(seeds, mapOf("cf.audit.other" to "x", "cf.settings.language" to "nl")))
    }
}
