package net.chronoflow.core.testing.junit

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor
import org.junit.platform.engine.support.descriptor.MethodSource
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.core.LauncherConfig
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runner.Runner
import org.junit.runner.notification.RunNotifier
import java.io.File
import java.nio.file.Files

class TimingListenerTest {
    @Test fun recordsOneClassAllowancePerSdkWithoutSpendingTheSdkAllowanceOnJvmTests() {
        val directory = Files.createTempDirectory(File("build").toPath(), "listener-proof-").toFile()
        val properties = mapOf("cf.tier" to "timing", "cf.timingsDir" to directory.absolutePath)
        val previous = properties.keys.associateWith(System::getProperty)
        val plan =
            LauncherFactory
                .create(LauncherConfig.builder().enableTestExecutionListenerAutoRegistration(false).build())
                .discover(LauncherDiscoveryRequestBuilder.request().build())
        try {
            properties.forEach { (key, value) -> System.setProperty(key, value) }
            val listener = TestTimingListener()

            fun execute(
                type: Class<*>,
                method: String,
                suffix: String = "",
            ) {
                val descriptor =
                    object : AbstractTestDescriptor(
                        UniqueId.forEngine("timing-proof").append("test", method),
                        method + suffix,
                        MethodSource.from(type.name, method),
                    ) {
                        override fun getType(): TestDescriptor.Type = TestDescriptor.Type.TEST
                    }
                val identifier = TestIdentifier.from(descriptor)
                listener.executionStarted(identifier)
                listener.executionFinished(identifier, TestExecutionResult.successful())
            }
            execute(TimingListenerTest::class.java, "jvmBefore")
            execute(TimingProofFirst::class.java, "sdkCold", "[36]")
            execute(TimingProofFirst::class.java, "sameClass", "[36][small-nl,ms=0]")
            execute(TimingProofSecond::class.java, "newClass", "[36]")
            execute(TimingProofSecond::class.java, "sameNewClass", "[36]")
            execute(TimingProofFirst::class.java, "newSdk", "[29]")
            execute(TimingProofFirst::class.java, "sameClassOnNewSdk", "[29]")
            execute(TimingProofSecond::class.java, "newClassOnOtherSdk", "[29]")
            execute(TimingProofSecond::class.java, "returnToEarlierSdk", "[36]")
            execute(TimingListenerTest::class.java, "jvmAfter")
            listener.testPlanExecutionFinished(plan)

            val report = File(directory, "${ProcessHandle.current().pid()}.json").readText()
            val records =
                Regex("""\{[^{}]*"method":"([^"]+)"[^{}]*}""").findAll(report).associate { match ->
                    val budget = Regex(""""budgetMs":(\d+)""").find(match.value)!!.groupValues[1].toLong()
                    val status = Regex(""""status":"([^"]+)"""").find(match.value)!!.groupValues[1]
                    match.groupValues[1] to (budget to status)
                }
            assertThat(records).isEqualTo(
                mapOf(
                    "jvmBefore" to (1_000L to "ok"),
                    "sdkCold" to (20_000L to "warmup"),
                    "sameClass" to (1_000L to "ok"),
                    "newClass" to (3_000L to "class-warmup"),
                    "sameNewClass" to (1_000L to "ok"),
                    "newSdk" to (20_000L to "warmup"),
                    "sameClassOnNewSdk" to (1_000L to "ok"),
                    "newClassOnOtherSdk" to (3_000L to "class-warmup"),
                    "returnToEarlierSdk" to (1_000L to "ok"),
                    "jvmAfter" to (1_000L to "ok"),
                ),
            )
        } finally {
            previous.forEach { (key, value) ->
                if (value == null) System.clearProperty(key) else System.setProperty(key, value)
            }
        }
    }
}

@RunWith(TimingProofRobolectricRunner::class)
private interface TimingProofFirst

@RunWith(TimingProofRobolectricRunner::class)
private interface TimingProofSecond

private class TimingProofRobolectricRunner : Runner() {
    override fun getDescription(): Description = Description.EMPTY

    override fun run(notifier: RunNotifier) = Unit
}
