package net.chronoflow.core.plan

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class PlanCopyResourceTest {
    private val bytes: ByteArray by lazy {
        checkNotNull(javaClass.getResourceAsStream("/plan_copy.json")) { "plan_copy.json is not on the classpath" }
            .use { it.readBytes() }
    }

    @Test fun planCopyIsOnTheClasspathWithThePinnedHash() {
        val recorded =
            File(System.getProperty("chronoflow.repoRoot"), "tools/l10n/plan_copy.sha256")
                .readText()
                .trim()
                .substringBefore(' ')
        val actual = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        assertThat(actual).isEqualTo(recorded)
        assertThat(bytes.size).isEqualTo(141_652)
    }

    @Test fun planCopyHas289Entries() {
        assertThat(Regex("\"key\"\\s*:").findAll(bytes.toString(Charsets.UTF_8)).count()).isEqualTo(289)
    }
}
