package net.chronoflow.core.network

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.time.Instant

class ApiErrorMapperTests {
    @Test fun conflictSentencesBecomeFormFields() {
        conflict("Two-factor authentication is already on.")
        conflict("Two-factor authentication is off.")
        conflict("Start two-factor setup again.")
        assertThat(map(409, "conflict")).isEqualTo(DataFailure.Conflict(409))
    }

    @Test fun codeMustMatchStatus() {
        assertThat(map(403, "unauthenticated")).isEqualTo(DataFailure.Server(403))
        assertThat(map(400, "validation_failed")).isEqualTo(DataFailure.Server(400))
        assertThat(map(422, "review_expired")).isEqualTo(DataFailure.ReviewExpired)
        assertThat(ApiErrorMapper.map(401, "<html>".encodeToByteArray(), null, Instant.EPOCH))
            .isEqualTo(DataFailure.Server(401))
    }

    @Test fun headerGrammarRejectsExponentsAndNonFiniteNumbers() {
        retry("1e3", 60.0)
        retry("NaN", 60.0)
        retry("12d", 60.0)
        retry("1.25", 1.25)
    }

    @Test fun numericErrorKeysDoNotCoerceToStrings() {
        val body = """{"code":"validation_failed","message":"bad","errors":{"a":123}}"""
        assertThat(ApiErrorMapper.map(422, body.encodeToByteArray(), null, Instant.EPOCH))
            .isEqualTo(DataFailure.Server(422))
    }

    private fun retry(
        header: String,
        seconds: Double,
    ) {
        assertThat(ApiErrorMapper.map(429, byteArrayOf(), header, Instant.EPOCH))
            .isEqualTo(DataFailure.Throttled(seconds, 429))
    }

    private fun conflict(message: String) {
        assertThat(map(409, "conflict", message))
            .isEqualTo(DataFailure.Validation(mapOf("form" to FieldMessage(message)), 409))
    }

    private fun map(
        status: Int,
        code: String,
        message: String = "Artificial envelope message.",
    ): DataFailure =
        ApiErrorMapper.map(status, """{"code":"$code","message":"$message"}""".encodeToByteArray(), null, Instant.EPOCH)
}
