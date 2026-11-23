package net.chronoflow.core.model.session

import org.junit.Assert.assertThrows
import org.junit.Test

class SessionMutationPermitTests {
    @Test fun invalidPermitRefuses() {
        val permit = SessionMutationPermit()
        permit.invalidate()
        assertThrows(StaleSessionException::class.java) { permit.whileValid { } }
    }

    @Test fun invalidateWaitsForAnAdmittedOperation() {
        val permit = SessionMutationPermit()
        assertInvalidationWaits({ permit.whileValid(it) }, permit::invalidate)
        assertThrows(StaleSessionException::class.java) { permit.whileValid { } }
    }
}
