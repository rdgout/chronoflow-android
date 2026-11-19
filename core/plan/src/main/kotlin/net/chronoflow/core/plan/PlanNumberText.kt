package net.chronoflow.core.plan

import net.chronoflow.core.model.contracts.Language
import java.math.BigDecimal
import java.math.RoundingMode

// Port of NumberFormatter en_GB / nl_NL with maximumFractionDigits = 3; BigDecimal.valueOf rounds the shortest decimal
// string of the double, as Apple does, where DecimalFormat would round the binary value.
internal fun formatPlanNumber(
    value: Double,
    language: Language,
): String {
    if (value.isNaN()) return "NaN"
    if (value.isInfinite()) return if (value > 0) "∞" else "-∞"
    val nl = language == Language.Nl
    val rounded = BigDecimal.valueOf(Math.abs(value)).setScale(3, RoundingMode.HALF_EVEN).stripTrailingZeros()
    val plain = rounded.toPlainString()
    val dot = plain.indexOf('.')
    val integer = if (dot < 0) plain else plain.substring(0, dot)
    val out = StringBuilder(plain.length + 6)
    if (value < 0.0 || (value == 0.0 && 1.0 / value < 0.0)) out.append('-')
    val groupSeparator = if (nl) '.' else ','
    for (i in integer.indices) {
        if (i > 0 && (integer.length - i) % 3 == 0) out.append(groupSeparator)
        out.append(integer[i])
    }
    if (dot >= 0) out.append(if (nl) ',' else '.').append(plain, dot + 1, plain.length)
    return out.toString()
}
