package net.chronoflow.core.testing.junit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class ClassWarmupTest {
    @Test fun recordsWarmupSeparatelyAndPreservesTheResult() =
        withDirectory {
            assertEquals(42, measureClassWarmup(javaClass, 36, "native \"fonts\"") { 42 })
            val report = Files.list(it).use { paths -> Files.readString(paths.toList().single()) }
            assertTrue(report.contains("\"tests\":[]"))
            assertTrue(report.contains("\"warmups\":[{\"class\":\"${javaClass.name}\",\"sdk\":\"36\""))
            assertTrue(report.contains("\"name\":\"native \\\"fonts\\\"\""))
            assertTrue(Regex("\"ms\":\\d+").containsMatchIn(report))
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
        val directory =
            Files.createTempDirectory(
                java.nio.file.Path
                    .of("build"),
                "class-warmup-proof-",
            )
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
