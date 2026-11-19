package net.chronoflow.core.testing.junit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.platform.commons.util.Preconditions
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor
import org.junit.platform.launcher.TestIdentifier
import java.io.File
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class TestHangWatchdogTest {
    @Test fun aHungTestWritesItsIdentityAndStacksBeforeHaltingTheWorker() {
        val directory = runWorker("hang", 86)
        val evidence = directory.listFiles()!!.single { it.name.startsWith("hang-") }.readText()
        assertTrue(evidence.contains("[engine:watchdog-proof]/[test:blockedTest]"))
        assertTrue(evidence.contains("WatchdogProofWorker.blockedTest"))
        assertTrue(evidence.contains("java.util.concurrent.CountDownLatch.await"))
        assertEquals(evidence, File(directory, "worker.log").readText())
    }

    @Test fun completingATestDisarmsItsWatchdog() {
        val directory = runWorker("finish", 0)
        assertFalse(directory.listFiles()!!.any { it.name.startsWith("hang-") })
        assertEquals("", File(directory, "worker.log").readText())
    }

    @Test fun aDebuggerLeavesTheWatchdogDisarmed() {
        val directory = runWorker("debug", 0)
        assertFalse(directory.listFiles()!!.any { it.name.startsWith("hang-") })
        assertTrue(File(directory, "worker.log").readText().contains("Listening for transport dt_socket"))
    }

    @Test fun theFirstTestCanWarmUpBeforeTheNormalCapApplies() {
        val directory = runWorker("warm-up", 86)
        val evidence = directory.listFiles()!!.single { it.name.startsWith("hang-") }.readText()
        assertTrue(evidence.contains("[test:warmTest] exceeded 100ms"))
    }

    @Test fun theCapIsConfigurableAndTheTimingTierUsesItsOwnBudgets() {
        assertEquals(60_000L, hangCapMillis("quick", null))
        assertEquals(125L, hangCapMillis("targeted", "0.125"))
        assertNull(hangCapMillis("timing", "0.125"))
    }

    private fun runWorker(
        mode: String,
        expectedExit: Int,
    ): File {
        val directory = Files.createTempDirectory(File("build").toPath(), "watchdog-proof-").toFile()
        val classpath =
            listOf(
                WatchdogProofWorker::class.java,
                TestTimingListener::class.java,
                TestIdentifier::class.java,
                TestDescriptor::class.java,
                Preconditions::class.java,
                Unit::class.java,
            ).map {
                File(
                    it.protectionDomain.codeSource.location
                        .toURI(),
                ).path
            }.distinct()
                .joinToString(File.pathSeparator)
        val command = mutableListOf(File(System.getProperty("java.home"), "bin/java").path)
        if (mode == "debug") {
            command += "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=127.0.0.1:0"
        }
        command +=
            listOf(
                "-Dcf.tier=targeted",
                "-Dcf.testHangCapSeconds=0.1",
                "-Dcf.timingsDir=${directory.absolutePath}",
                "-cp",
                classpath,
                WatchdogProofWorker::class.java.name,
                mode,
            )
        val process =
            ProcessBuilder(command)
                .redirectErrorStream(true)
                .redirectOutput(File(directory, "worker.log"))
                .start()
        try {
            assertTrue("watchdog proof worker did not exit", process.waitFor(3, TimeUnit.SECONDS))
            assertEquals(File(directory, "worker.log").readText(), expectedExit, process.exitValue())
        } finally {
            if (process.isAlive) process.destroyForcibly().waitFor(3, TimeUnit.SECONDS)
        }
        return directory
    }
}

internal object WatchdogProofWorker {
    @JvmStatic fun main(args: Array<String>) {
        fun identifier(name: String): TestIdentifier {
            val descriptor =
                object : AbstractTestDescriptor(
                    UniqueId.forEngine("watchdog-proof").append("test", name),
                    name,
                ) {
                    override fun getType(): TestDescriptor.Type = TestDescriptor.Type.TEST
                }
            return TestIdentifier.from(descriptor)
        }
        val mode = args.single()
        val listener = TestTimingListener()
        val cold = identifier("coldTest")
        listener.executionStarted(cold)
        if (mode == "warm-up") CountDownLatch(1).await(200, TimeUnit.MILLISECONDS)
        listener.executionFinished(cold, TestExecutionResult.successful())
        val test = identifier(if (mode == "warm-up") "warmTest" else "blockedTest")
        listener.executionStarted(test)
        when (mode) {
            "finish" -> {
                listener.executionFinished(test, TestExecutionResult.successful())
                CountDownLatch(1).await(250, TimeUnit.MILLISECONDS)
            }

            "debug" -> {
                CountDownLatch(1).await(250, TimeUnit.MILLISECONDS)
                listener.executionFinished(test, TestExecutionResult.successful())
            }

            else -> {
                blockedTest()
            }
        }
    }

    fun blockedTest() {
        CountDownLatch(1).await()
    }
}
