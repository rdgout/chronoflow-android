package net.chronoflow.core.network

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.api.ApiErrorEnvelope
import net.chronoflow.core.model.api.ApiJson
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

internal object ApiErrorMapper {
    fun map(
        status: Int,
        body: ByteArray,
        retryAfterHeader: String?,
        now: Instant,
    ): DataFailure {
        val raw =
            try {
                ApiJson.response.parseToJsonElement(body.decodeToString()) as? JsonObject
            } catch (_: IllegalArgumentException) {
                null
            }
        if (status == 429) {
            val value = raw?.get("retryAfterSeconds") as? JsonPrimitive
            val seconds = value?.takeUnless { it.isString }?.doubleOrNull?.takeIf { it.isFinite() && it >= 0 }
            return DataFailure.Throttled(seconds ?: retryAfter(retryAfterHeader, now) ?: 60.0, status)
        }
        val envelope =
            try {
                ApiJson.response.decodeFromString(ApiErrorEnvelope.serializer(), body.decodeToString())
            } catch (_: IllegalArgumentException) {
                null
            } ?: return DataFailure.Server(status)

        // kotlinx accepts unquoted numbers as Strings; Swift's Decodable does not.
        fun isString(value: kotlinx.serialization.json.JsonElement?): Boolean = value is JsonPrimitive && value.isString
        if (!isString(raw?.get("code")) || !isString(raw?.get("message"))) return DataFailure.Server(status)
        return when (status to envelope.code) {
            401 to "unauthenticated" -> {
                DataFailure.Unauthorized(status)
            }

            403 to "email_unverified" -> {
                DataFailure.EmailUnverified(status)
            }

            422 to "review_expired" -> {
                DataFailure.ReviewExpired
            }

            422 to "validation_failed" -> {
                val errors = envelope.errors
                val rawErrors = raw?.get("errors") as? JsonObject
                val validArgs =
                    envelope.args.orEmpty().values.all { args ->
                        args.values.all { value ->
                            value is JsonPrimitive && value != JsonNull &&
                                (value.isString || (value.doubleOrNull != null && value.booleanOrNull == null))
                        }
                    }
                if (errors.isNullOrEmpty() || errors.any { it.key.isEmpty() || it.value.isEmpty() } ||
                    rawErrors == null || rawErrors.values.any { !isString(it) } || !validArgs
                ) {
                    DataFailure.Server(status)
                } else {
                    DataFailure.Validation(
                        errors.mapValues { (field, key) ->
                            FieldMessage(
                                key,
                                envelope.args
                                    ?.get(field)
                                    .orEmpty()
                                    .mapValues { it.value.jsonPrimitive },
                            )
                        },
                        status,
                    )
                }
            }

            409 to "conflict" -> {
                if (envelope.message in conflictMessages) {
                    DataFailure.Validation(mapOf("form" to FieldMessage(envelope.message)), status)
                } else {
                    DataFailure.Conflict(status)
                }
            }

            else -> {
                DataFailure.Server(status)
            }
        }
    }

    private fun retryAfter(
        header: String?,
        now: Instant,
    ): Double? {
        if (header == null) return null
        if (secondsPattern.matches(header)) {
            header.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }?.let { return it }
        }
        return try {
            val date = ZonedDateTime.parse(header, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()
            val duration = java.time.Duration.between(now, date)
            maxOf(0.0, duration.seconds.toDouble() + duration.nano / 1_000_000_000.0)
        } catch (_: DateTimeParseException) {
            null
        }
    }

    private val secondsPattern = Regex("^[0-9]+(\\.[0-9]+)?$")
    private val conflictMessages =
        setOf(
            "Two-factor authentication is already on.",
            "Two-factor authentication is off.",
            "Start two-factor setup again.",
        )
}
