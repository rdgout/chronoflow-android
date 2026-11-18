package net.chronoflow.core.datamock

import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** Android atomic store coverage (new). */
class FileMockStateStoreTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun stateUsesMockServerPathAndAtomicWrite() {
        val store = FileMockStateStore(temporary.root)
        assertTrue(store.file.path.endsWith("ChronoFlowMock/Server/state.json"))
        assertNull(store.load())
        val state = codecState()
        store.save(state)
        assertEquals(state, store.load())
        assertFalse(File(store.file.parentFile, "state.json.tmp").exists())
    }

    @Test fun corruptStateThrowsWithoutExposingInput() {
        val store = FileMockStateStore(temporary.root)
        store.file.parentFile.mkdirs()
        store.file.writeText("{artificial-secret")
        val failure = assertThrows(SerializationException::class.java) { store.load() }
        assertEquals("invalid mock server state", failure.message)
        assertNull(failure.cause)
    }
}
