package net.chronoflow.core.model.guest

import net.chronoflow.core.model.session.StorageNamespace
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class GuestRecoveryLocationTest {
    @Test
    fun pathPerNamespace() {
        val root = File("build/guest-location-tests")
        assertEquals(File(root, "ChronoFlow/guest-wizard.json"), GuestRecoveryLocation.file(root, StorageNamespace.Api))
        assertEquals(
            File(root, "ChronoFlowMock/guest-wizard.json"),
            GuestRecoveryLocation.file(root, StorageNamespace.Mock),
        )
    }
}
