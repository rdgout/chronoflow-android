package net.chronoflow.tools.tokengen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files

class OutputsTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun staleOutputIsRejected() {
        val root = folder.newFolder().toPath()
        val outputs = mapOf("probe.json" to "expected".toByteArray(), "other.txt" to "x".toByteArray())
        writeOutputs(outputs, root, check = false)
        Files.writeString(root.resolve("probe.json"), "stale")
        val error = assertThrows(GenerationError::class.java) { writeOutputs(outputs, root, check = true) }
        assertEquals("stale generated output: probe.json", error.message)
        assertTrue(Files.exists(root.resolve("tools/tokengen/build/verify/probe.json")))
    }

    @Test
    fun missingOutputIsRejectedAndEveryStalePathIsNamed() {
        val root = folder.newFolder().toPath()
        val outputs = mapOf("a.txt" to "a".toByteArray(), "b.txt" to "b".toByteArray())
        val error = assertThrows(GenerationError::class.java) { writeOutputs(outputs, root, check = true) }
        assertEquals("stale generated output: a.txt\nstale generated output: b.txt", error.message)
    }

    @Test
    fun matchingOutputsPass() {
        val root = folder.newFolder().toPath()
        val outputs = mapOf("a.txt" to "a".toByteArray())
        writeOutputs(outputs, root, check = false)
        writeOutputs(outputs, root, check = true)
    }
}
