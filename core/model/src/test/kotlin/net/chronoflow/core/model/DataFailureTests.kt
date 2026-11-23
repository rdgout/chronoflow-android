package net.chronoflow.core.model

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import org.junit.Test

class DataFailureTests {
    @Test
    fun statusMatchesSwift() {
        assertThat(DataFailure.ReviewExpired.status).isEqualTo(422)
        assertThat(DataFailure.NotEnoughCredits.status).isEqualTo(422)
        assertThat(DataFailure.SaveCapacity.status).isEqualTo(422)
        assertThat(DataFailure.ProfileInvalid(emptyMap()).status).isEqualTo(422)
        assertThat(DataFailure.Offline.status).isNull()
        assertThat(DataFailure.Unauthorized(401).status).isEqualTo(401)
        assertThat(DataFailure.EmailUnverified(403).status).isEqualTo(403)
        assertThat(DataFailure.Validation(emptyMap(), 422).status).isEqualTo(422)
        assertThat(DataFailure.Throttled(1.0, 429).status).isEqualTo(429)
        assertThat(DataFailure.Conflict(409).status).isEqualTo(409)
        assertThat(DataFailure.Server(null).status).isNull()
        assertThat(DataFailure.Server(500).status).isEqualTo(500)
        assertThat(DataFailure.Unsupported(OutboxKind.Profile).status).isNull()
    }
}
