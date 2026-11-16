package net.chronoflow.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import java.math.BigDecimal

@Serializable
data class FieldMessage(
    val key: String,
    val arguments: Map<String, JsonPrimitive>,
) {
    constructor(key: String) : this(key, emptyMap())

    private val interpolationNames: List<String>
        get() = arguments.keys.filter { key.contains(":$it") || key.contains("{{$it}}") }.sorted()

    val catalogArguments: List<JsonPrimitive>
        get() = interpolationNames.mapNotNull { arguments[it] }

    // iOS placeholder syntax on purpose: this is the lookup key into the copy index.
    val interpolationKey: String
        get() {
            val names = interpolationNames
            var result = key
            names.forEachIndexed { index, name ->
                val type = placeholderType(arguments.getValue(name))
                val placeholder = if (names.size == 1) "%$type" else "%${index + 1}$$type"
                result = result.replace(":$name", placeholder).replace("{{$name}}", placeholder)
            }
            return result
        }

    private fun placeholderType(value: JsonPrimitive): String {
        val number = if (value.isString) null else value.content.toBigDecimalOrNull()
        return when {
            number == null -> "@"
            number.stripTrailingZeros().scale() <= 0 && number.abs() <= BigDecimal(Long.MAX_VALUE) -> "lld"
            else -> "f"
        }
    }
}

sealed class DataFailure : Exception(null, null, false, false) {
    abstract val status: Int?

    data object ReviewExpired : DataFailure() {
        override val status: Int? = 422
    }

    data object NotEnoughCredits : DataFailure() {
        override val status: Int? = 422
    }

    data object SaveCapacity : DataFailure() {
        override val status: Int? = 422
    }

    data class ProfileInvalid(
        val fields: Map<String, FieldMessage>,
    ) : DataFailure() {
        override val status: Int? = 422
    }

    data object Offline : DataFailure() {
        override val status: Int? = null
    }

    data class Unauthorized(
        override val status: Int,
    ) : DataFailure()

    data class EmailUnverified(
        override val status: Int,
    ) : DataFailure()

    data class Validation(
        val fields: Map<String, FieldMessage>,
        override val status: Int,
    ) : DataFailure()

    data class Throttled(
        val retryAfterSeconds: Double,
        override val status: Int,
    ) : DataFailure()

    data class Conflict(
        override val status: Int,
    ) : DataFailure()

    data class Server(
        override val status: Int?,
    ) : DataFailure()

    data class Unsupported(
        val kind: OutboxKind,
    ) : DataFailure() {
        override val status: Int? = null
    }
}
