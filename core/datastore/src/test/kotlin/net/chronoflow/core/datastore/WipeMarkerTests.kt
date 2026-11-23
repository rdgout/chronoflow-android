package net.chronoflow.core.datastore

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.AccountID
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test
import java.nio.file.Files
import kotlin.time.Duration.Companion.seconds

class WipeMarkerTests {
    private val root = Files.createTempDirectory("wipe-marker").toFile()
    private val marker = WipeMarker(root, Dispatchers.IO)
    private val accountA = AccountID.of("019a562b-8000-7000-8000-000000000001")

    @After fun cleanup() {
        root.deleteRecursively()
    }

    @Test fun markerIsAJsonString() =
        runTest(timeout = 60.seconds) {
            marker.write(accountA)
            assertEquals("\"${accountA.rawValue}\"", marker.file.readText())
        }

    @Test fun readRoundTripsAndClearRemoves() =
        runTest(timeout = 60.seconds) {
            assertNull(marker.read())
            marker.write(accountA)
            assertEquals(accountA, marker.read())
            marker.clear()
            marker.clear()
            assertNull(marker.read())
        }

    @Test fun corruptMarkerThrowsAndIsKept() =
        runTest(timeout = 60.seconds) {
            for (value in listOf("\"../outside\"", "not json", "42")) {
                marker.file.writeText(value)
                try {
                    marker.read()
                    fail()
                } catch (_: CorruptWipeMarkerException) {
                }
                assertEquals(value, marker.file.readText())
            }
        }

    @Test fun noTmpFileIsLeft() =
        runTest(timeout = 60.seconds) {
            marker.write(accountA)
            marker.write(accountA)
            assertEquals(listOf(marker.file.name), root.list()!!.toList())
        }
}
