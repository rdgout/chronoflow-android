package net.chronoflow.feature.settings.security

import net.chronoflow.core.model.store.SettingsRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SecuritySummaryTests {
    @Test fun missingRowIsNull() {
        assertNull(SecuritySummary.from(emptyList<SettingsRecord>()))
    }

    @Test fun stringTrueIsFalse() {
        assertFalse(summary("{\"twoFactorEnabled\":\"true\"}").twoFactorEnabled)
    }

    @Test fun booleanTrueIsTrue() {
        assertTrue(summary("{\"twoFactorEnabled\":true}").twoFactorEnabled)
    }

    @Test fun absentCountIsNull() {
        assertNull(summary("{}").passkeyCount)
    }

    @Test fun integerCountIsAccepted() {
        assertEquals(2, summary("{\"passkeyCount\":2}").passkeyCount)
    }

    @Test fun fractionalCountIsNull() {
        assertNull(summary("{\"passkeyCount\":2.5}").passkeyCount)
    }

    @Test fun negativeCountIsNull() {
        assertNull(summary("{\"passkeyCount\":-1}").passkeyCount)
    }

    @Test fun stringCountIsNull() {
        assertNull(summary("{\"passkeyCount\":\"2\"}").passkeyCount)
    }

    private fun summary(raw: String) =
        checkNotNull(
            SecuritySummary.from(
                listOf(
                    SettingsRecord(
                        "security",
                        raw,
                    ),
                ),
            ),
        )
}
