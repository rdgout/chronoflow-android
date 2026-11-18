package net.chronoflow.core.datamock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class MockGuestRegistrationClaimTests {
    @Test fun legacyClaimDecodesAndReencodesWithoutPlaintextSecret() {
        val old = """{"accountID":"owner","tripID":"trip","secret":"legacy-secret"}"""
        val claim = MockJson.decodeFromString(MockClaim.serializer(), old)
        assertEquals(MockGuestReview.hash("legacy-secret"), claim.secretHash)
        val stored = MockJson.encodeToString(MockClaim.serializer(), claim)
        assertFalse(stored.contains("legacy-secret"))
    }
}
