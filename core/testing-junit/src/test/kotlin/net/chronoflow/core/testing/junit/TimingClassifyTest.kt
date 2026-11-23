package net.chronoflow.core.testing.junit

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class TimingClassifyTest {
    @Test fun parameterizedCasesKeepTheirSdkInsteadOfClaimingAnotherColdAllowance() =
        assertThat(sdkFromDisplayName("case[36][unauthorized=true]")).isEqualTo("36")

    @Test fun jvmOverOneSecond() =
        assertThat(
            classify("jvm", "default", 1_200, false, false),
        ).isEqualTo(1_000L to "over")

    @Test fun robolectricWithinOneSecond() =
        assertThat(classify("robolectric", "36", 900, false, false)).isEqualTo(1_000L to "ok")

    @Test fun firstRobolectricPerSdkIsWarmup() =
        assertThat(classify("robolectric", "36", 12_000, false, true)).isEqualTo(20_000L to "warmup")

    @Test fun secondRobolectricOnSameSdkIsOver() =
        assertThat(classify("robolectric", "36", 1_500, false, false)).isEqualTo(1_000L to "over")

    @Test fun firstRobolectricPerClassGetsThreeSeconds() =
        assertThat(classify("robolectric", "36", 2_500, false, false, true)).isEqualTo(3_000L to "class-warmup")

    @Test fun classWarmupOverThreeSecondsIsOver() =
        assertThat(classify("robolectric", "36", 3_500, false, false, true)).isEqualTo(3_000L to "over")

    @Test fun secondCaseInTheClassKeepsOneSecond() =
        assertThat(classify("robolectric", "36", 1_100, false, false, false)).isEqualTo(1_000L to "over")

    @Test fun sdkWarmupTakesPrecedenceOverClassWarmup() =
        assertThat(classify("robolectric", "36", 12_000, false, true, true)).isEqualTo(20_000L to "warmup")

    @Test fun classWarmupDoesNotExtendJvmCases() =
        assertThat(classify("jvm", "default", 1_100, false, false, true)).isEqualTo(1_000L to "over")

    @Test fun firstOnAnotherSdkIsWarmup() =
        assertThat(classify("robolectric", "29", 9_000, false, true)).isEqualTo(20_000L to "warmup")

    @Test fun perfBudgetIsExempt() =
        assertThat(
            classify("robolectric", "36", 6_000, true, false),
        ).isEqualTo(null to "exempt")

    @Test fun warmupOverTwentySecondsIsOver() =
        assertThat(classify("robolectric", "36", 21_000, false, true)).isEqualTo(20_000L to "over")
}
