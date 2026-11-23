package net.chronoflow.konsist

import assertk.assertFailure
import assertk.assertions.messageContains
import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.container.KoScope
import org.junit.BeforeClass
import org.junit.Test

class TierRegistryTest : KonsistTest() {
    private fun allowed(path: String): KoScope = Konsist.scopeFromFile("konsist/src/test/resources/allowed/tier/$path")

    private fun planted(path: String): KoScope =
        Konsist.scopeFromFile("konsist/src/test/resources/violations/tier/$path")

    @Test fun abstractTestClassesCarryNoTier() = abstractTestClassesCarryNoTier(projectScope)

    @Test fun tieredAbstractBaseIsRejected() {
        assertFailure {
            abstractTestClassesCarryNoTier(
                planted("app/src/test/kotlin/net/chronoflow/app/TieredAbstractBaseTest.kt"),
            )
        }.messageContains("TieredAbstractBaseTest")
    }

    @Test fun plainAbstractBaseIsAllowed() =
        abstractTestClassesCarryNoTier(allowed("app/src/test/kotlin/net/chronoflow/app/PlainAbstractBaseTest.kt"))

    @Test fun timingGateModulesAreListed() = timingGateModulesAreListed(projectScope, timingModulesInMakefile())

    @Test fun timingGateInUnlistedModuleIsRejected() {
        assertFailure {
            timingGateModulesAreListed(
                planted("core/plan/src/test/kotlin/net/chronoflow/core/plan/UnlistedTimingGateTest.kt"),
                emptySet(),
            )
        }.messageContains("UnlistedTimingGateTest")
    }

    @Test fun timingGateInListedModuleIsAllowed() =
        timingGateModulesAreListed(
            allowed("core/plan/src/test/kotlin/net/chronoflow/core/plan/ListedTimingGateTest.kt"),
            setOf("core:plan"),
        )

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

    @Test fun emulatorClassesHaveSmokeCaptureOrAudit() = emulatorClassesHaveSmokeCaptureOrAudit(projectScope)

    @Test fun untaggedEmulatorClassIsRejected() {
        assertFailure {
            emulatorClassesHaveSmokeCaptureOrAudit(
                planted("app/src/androidTest/kotlin/net/chronoflow/app/UntaggedEmulatorTest.kt"),
            )
        }.messageContains("UntaggedEmulatorTest")
    }

    @Test fun auditIsEmulatorOnly() = auditIsEmulatorOnly(projectScope)

    @Test fun auditEmulatorClassIsAllowed() =
        emulatorClassesHaveSmokeCaptureOrAudit(
            allowed("app/src/androidTest/kotlin/net/chronoflow/app/AuditEmulatorTest.kt"),
        )

    @Test fun twoEmulatorTiersAreRejected() {
        assertFailure {
            emulatorClassesHaveSmokeCaptureOrAudit(
                planted("app/src/androidTest/kotlin/net/chronoflow/app/SmokeAndAuditTest.kt"),
            )
        }.messageContains("SmokeAndAuditTest")
    }

    @Test fun auditOnJvmIsRejected() {
        assertFailure {
            auditIsEmulatorOnly(planted("app/src/test/kotlin/net/chronoflow/app/AuditJvmTest.kt"))
        }.messageContains("AuditJvmTest")
    }

    @Test fun auditOnRobolectricIsRejected() {
        assertFailure {
            auditIsEmulatorOnly(planted("app/src/test/kotlin/net/chronoflow/app/AuditRobolectricTest.kt"))
        }.messageContains("AuditRobolectricTest")
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
        lateinit var projectScope: KoScope

        @BeforeClass
        @JvmStatic
        fun buildProjectScope() {
            projectScope = resolvedProjectScope()
        }
    }
}
