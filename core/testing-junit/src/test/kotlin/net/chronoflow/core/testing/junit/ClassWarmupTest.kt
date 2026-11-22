package net.chronoflow.core.testing.junit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files

class ClassWarmupTest {
    @get:Rule val temporary = TemporaryFolder.builder().assureDeletion().build()

    @Test fun recordsWarmupSeparatelyAndPreservesTheResult() =
        withDirectory {
            assertEquals(42, measureClassWarmup(javaClass, 36, "native \"fonts\"") { 42 })
            val report = Files.list(it).use { paths -> Files.readString(paths.toList().single()) }
            assertTrue(report.contains("\"tests\":[]"))
            assertTrue(report.contains("\"warmups\":[{\"class\":\"${javaClass.name}\",\"sdk\":\"36\""))
            assertTrue(report.contains("\"name\":\"native \\\"fonts\\\"\""))
            assertTrue(Regex("\"ms\":\\d+").containsMatchIn(report))
            val started = Regex("\"startedNanos\":(\\d+)").find(report)!!.groupValues[1].toLong()
            val ended = Regex("\"endedNanos\":(\\d+)").find(report)!!.groupValues[1].toLong()
            assertTrue(ended >= started)
        }

    @Test fun recordsDurationWhenTheWarmupFails() =
        withDirectory {
            val failure = IllegalStateException("fixture failed")
            assertEquals(
                failure,
                assertThrows(IllegalStateException::class.java) {
                    measureClassWarmup(javaClass, 36, "fonts") { throw failure }
                },
            )
            assertEquals(1, Files.list(it).use { paths -> paths.count() }.toInt())
        }

    private fun withDirectory(block: (java.nio.file.Path) -> Unit) {
        val directory = temporary.newFolder().toPath()
        val previous = System.getProperty("cf.timingsDir")
        try {
            System.setProperty("cf.timingsDir", directory.toString())
            block(directory)
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
}
