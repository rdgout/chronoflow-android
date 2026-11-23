package net.chronoflow.core.testing.junit

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class TierNameTest {
    private val classTiers = listOf("Logic", "Layout", "Snapshot", "Capture", "Smoke", "Perf", "TimingGate", null)

    private val expected: Map<TierName, Set<String?>> =
        mapOf(
            TierName.Quick to setOf(null, "Logic"),
            TierName.Layout to setOf("Layout"),
            TierName.Test to setOf(null, "Logic", "Layout"),
            TierName.Snapshot to setOf("Snapshot"),
            TierName.Capture to setOf("Capture"),
            TierName.Timing to setOf("TimingGate"),
            TierName.Targeted to classTiers.toSet(),
        )

    @Test
    fun everyTierRunsExactlyItsClasses() {
        for (tier in TierName.entries) {
            val actual = classTiers.filter { tier.includes(it) }.toSet()
            assertThat(actual).isEqualTo(expected.getValue(tier))
        }
    }

    @Test
    fun parseIsCaseInsensitiveAndFallsBackToQuick() {
        assertThat(TierName.parse(null)).isEqualTo(TierName.Quick)
        assertThat(TierName.parse("LAYOUT")).isEqualTo(TierName.Layout)
        assertThat(TierName.parse("bogus")).isEqualTo(TierName.Quick)
    }
}
