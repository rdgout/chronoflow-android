package net.chronoflow.core.model.contracts

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import java.math.BigDecimal

val JsonElement?.int: Int?
    get() {
        val primitive = this as? JsonPrimitive ?: return null
        if (primitive.isString) return null
        val number = primitive.content.toBigDecimalOrNull() ?: return null
        val whole = number.stripTrailingZeros()
        val fits = whole.signum() >= 0 && whole.scale() <= 0 && whole <= BigDecimal(Int.MAX_VALUE)
        return if (fits) whole.toInt() else null
    }
