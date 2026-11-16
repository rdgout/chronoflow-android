package net.chronoflow.core.testing.junit

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class TimingClassifyTest {
    @Test fun jvmOverOneSecond() =
        assertThat(
            classify("jvm", "default", 1_200, false, false),
        ).isEqualTo(1_000L to "over")

    @Test fun robolectricWithinFiveSeconds() =
        assertThat(classify("robolectric", "36", 4_900, false, false)).isEqualTo(5_000L to "ok")

    @Test fun firstRobolectricPerSdkIsWarmup() =
        assertThat(classify("robolectric", "36", 12_000, false, true)).isEqualTo(20_000L to "warmup")

    @Test fun secondRobolectricOnSameSdkIsOver() =
        assertThat(classify("robolectric", "36", 6_000, false, false)).isEqualTo(5_000L to "over")

    @Test fun firstOnAnotherSdkIsWarmup() =
        assertThat(classify("robolectric", "29", 9_000, false, true)).isEqualTo(20_000L to "warmup")

    @Test fun perfBudgetIsExempt() =
        assertThat(
            classify("robolectric", "36", 6_000, true, false),
        ).isEqualTo(null to "exempt")

    @Test fun warmupOverTwentySecondsIsOver() =
        assertThat(classify("robolectric", "36", 21_000, false, true)).isEqualTo(20_000L to "over")
}
