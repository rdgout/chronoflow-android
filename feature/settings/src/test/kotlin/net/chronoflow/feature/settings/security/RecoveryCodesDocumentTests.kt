package net.chronoflow.feature.settings.security

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class RecoveryCodesDocumentTests {
    @Test fun bytesAreUtf8LfWithoutBomOrTrailingNewline() {
        assertArrayEquals(
            "8f3k-29dm\nq7zp-4wxr".toByteArray(Charsets.UTF_8),
            RecoveryCodesDocument.bytes(listOf("8f3k-29dm", "q7zp-4wxr")),
        )
    }
}
