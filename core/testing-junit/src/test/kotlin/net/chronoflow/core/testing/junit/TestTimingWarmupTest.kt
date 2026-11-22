package net.chronoflow.core.testing.junit

import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor
import org.junit.platform.engine.support.descriptor.MethodSource
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import org.junit.rules.TemporaryFolder
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runner.Runner
import org.junit.runner.notification.RunNotifier
import java.nio.file.Files

class TestTimingWarmupTest {
    @get:Rule val temporary = TemporaryFolder.builder().assureDeletion().build()

    @Test fun labelingSlowTestWorkAsWarmupDoesNotHideEitherCaseBudget() {
        val directory = temporary.newFolder().toPath()
        val previous = System.getProperty("cf.timingsDir")
        try {
            System.setProperty("cf.timingsDir", directory.toString())
            var now = 0L
            val listener = TestTimingListener { now }
            val cold = identifier(WarmupProofColdCase::class.java, "cold")
            listener.executionStarted(cold)
            listener.executionFinished(cold, TestExecutionResult.successful())
            val first = identifier(WarmupProofSlowCase::class.java, "first")
            listener.executionStarted(first)
            measureClassWarmup(WarmupProofSlowCase::class.java, 36, "mislabelled first-case body", { now }) {
                now += 3_100_000_000
            }
            listener.executionFinished(first, TestExecutionResult.successful())
            val second = identifier(WarmupProofSlowCase::class.java, "second")
            listener.executionStarted(second)
            measureClassWarmup(WarmupProofSlowCase::class.java, 36, "mislabelled warm body", { now }) {
                now += 1_100_000_000
            }
            listener.executionFinished(second, TestExecutionResult.successful())
            val plan = LauncherFactory.create().discover(LauncherDiscoveryRequestBuilder.request().build())
            listener.testPlanExecutionFinished(plan)
            val report = Files.readString(directory.resolve("${ProcessHandle.current().pid()}.json"))
            assertTrue(
                report.contains(
                    "\"method\":\"first\",\"kind\":\"robolectric\",\"sdk\":\"36\",\"ms\":3100,\"budgetMs\":3000,\"status\":\"over\"",
                ),
            )
            assertTrue(
                report.contains(
                    "\"method\":\"second\",\"kind\":\"robolectric\",\"sdk\":\"36\",\"ms\":1100,\"budgetMs\":1000,\"status\":\"over\"",
                ),
            )
        } finally {
            if (previous ==
                null
            ) {
                System.clearProperty("cf.timingsDir")
            } else {
                System.setProperty("cf.timingsDir", previous)
            }
        }
    }

    private fun identifier(
        type: Class<*>,
        name: String,
    ): TestIdentifier =
        TestIdentifier.from(
            object : AbstractTestDescriptor(
                UniqueId.forEngine("warmup-proof").append("test", name),
                "$name[36]",
                MethodSource.from(type.name, name),
            ) {
                override fun getType(): TestDescriptor.Type = TestDescriptor.Type.TEST
            },
        )
}

@RunWith(WarmupProofRobolectricRunner::class)
private interface WarmupProofColdCase

@RunWith(WarmupProofRobolectricRunner::class)
private interface WarmupProofSlowCase

private class WarmupProofRobolectricRunner : Runner() {
    override fun getDescription(): Description = Description.EMPTY

    override fun run(notifier: RunNotifier) = Unit
}
