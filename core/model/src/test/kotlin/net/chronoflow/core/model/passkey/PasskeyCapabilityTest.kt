package net.chronoflow.core.model.passkey

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PasskeyCapabilityTest {
    @Test fun shippedCapabilityIsOff() {
        assertFalse(PasskeyCapability.SERVER_READY)
        assertFalse(PasskeyCapability.Shipped.enabled)
    }

    @Test fun canceledHasNoMessageOrStackTrace() {
        assertNull(PasskeyFailure.Canceled.message)
        assertTrue(PasskeyFailure.Canceled.stackTrace.isEmpty())
    }

    @Test fun invalidEncodingHasNoMessageOrStackTrace() {
        assertNull(PasskeyFailure.InvalidEncoding.message)
        assertTrue(PasskeyFailure.InvalidEncoding.stackTrace.isEmpty())
    }

    @Test fun unavailableHasNoMessageOrStackTrace() {
        assertNull(PasskeyFailure.Unavailable.message)
        assertTrue(PasskeyFailure.Unavailable.stackTrace.isEmpty())
    }

    @Test fun busyHasNoMessageOrStackTrace() {
        assertNull(PasskeyFailure.Busy.message)
        assertTrue(PasskeyFailure.Busy.stackTrace.isEmpty())
    }

    @Test fun alreadyRegisteredHasNoMessageOrStackTrace() {
        assertNull(PasskeyFailure.AlreadyRegistered.message)
        assertTrue(PasskeyFailure.AlreadyRegistered.stackTrace.isEmpty())
    }
}
