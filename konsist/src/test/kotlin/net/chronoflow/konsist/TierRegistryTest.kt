package net.chronoflow.konsist

import assertk.assertFailure
import assertk.assertions.messageContains
import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.container.KoScope
import org.junit.Test

class TierRegistryTest {
    private fun planted(path: String): KoScope =
        Konsist.scopeFromFile("konsist/src/test/resources/violations/tier/$path")

    @Test fun robolectricClassesHaveOneTier() = robolectricClassesHaveOneTier(projectScope)

    @Test fun untaggedRobolectricClassIsRejected() {
        assertFailure {
            robolectricClassesHaveOneTier(planted("app/src/test/kotlin/net/chronoflow/app/UntaggedRobolectricTest.kt"))
        }.messageContains("UntaggedRobolectricTest")
    }

    @Test fun twoTierRobolectricClassIsRejected() {
        assertFailure {
            robolectricClassesHaveOneTier(planted("app/src/test/kotlin/net/chronoflow/app/TwoTierRobolectricTest.kt"))
        }.messageContains("TwoTierRobolectricTest")
    }

    @Test fun emulatorClassesHaveSmokeOrCapture() = emulatorClassesHaveSmokeOrCapture(projectScope)

    @Test fun untaggedEmulatorClassIsRejected() {
        assertFailure {
            emulatorClassesHaveSmokeOrCapture(
                planted("app/src/androidTest/kotlin/net/chronoflow/app/UntaggedEmulatorTest.kt"),
            )
        }.messageContains("UntaggedEmulatorTest")
    }

    @Test fun benchmarkClassesArePerf() = benchmarkClassesArePerf(projectScope)

    @Test fun untaggedBenchmarkClassIsRejected() {
        assertFailure {
            benchmarkClassesArePerf(planted("benchmark/src/main/kotlin/net/chronoflow/benchmark/UntaggedBenchmark.kt"))
        }.messageContains("UntaggedBenchmark")
    }

    @Test fun plainJvmClassesHaveNoTier() = plainJvmClassesHaveNoTier(projectScope)

    @Test fun layoutOnPlainJvmIsRejected() {
        assertFailure {
            plainJvmClassesHaveNoTier(planted("core/plan/src/test/kotlin/net/chronoflow/core/plan/LayoutOnJvmTest.kt"))
        }.messageContains("LayoutOnJvmTest")
    }

    @Test fun runTestHasTimeout() = runTestHasTimeout(projectScope)

    @Test fun runTestWithoutTimeoutIsRejected() {
        assertFailure {
            runTestHasTimeout(
                planted("core/sync/src/test/kotlin/net/chronoflow/core/sync/RunTestWithoutTimeoutTest.kt"),
            )
        }.messageContains("RunTestWithoutTimeoutTest")
    }

    @Test fun noTimeoutRuleOutsideEmulatorTests() = noTimeoutRuleOutsideEmulatorTests(projectScope)

    @Test fun timeoutRuleInRobolectricIsRejected() {
        assertFailure {
            noTimeoutRuleOutsideEmulatorTests(
                planted("app/src/test/kotlin/net/chronoflow/app/TimeoutRuleInRobolectricTest.kt"),
            )
        }.messageContains("TimeoutRuleInRobolectricTest")
    }

    private companion object {
        val projectScope: KoScope by lazy { projectScope() }
    }
}
