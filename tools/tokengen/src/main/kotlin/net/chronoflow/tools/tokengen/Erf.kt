package net.chronoflow.tools.tokengen

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sqrt

/**
 * The error function by its all-positive power series, erf(x) = 2/sqrt(pi) * exp(-x^2) * sum 2^n x^(2n+1) / (2n+1)!!.
 * It has no cancellation, so it stays within a few ulp of libm for |x| < 6, where erf is 1 to double precision.
 */
fun erf(x: Double): Double {
    val a = abs(x)
    if (a >= 6.0) return if (x < 0) -1.0 else 1.0
    val square = a * a
    var term = a
    var sum = a
    var n = 0
    while (term > sum * 1e-17) {
        n++
        term *= 2 * square / (2 * n + 1)
        sum += term
    }
    val value = 2 / sqrt(PI) * exp(-square) * sum
    return if (x < 0) -value else value
}
