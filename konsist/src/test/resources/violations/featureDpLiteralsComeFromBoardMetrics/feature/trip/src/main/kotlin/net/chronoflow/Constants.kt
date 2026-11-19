package net.chronoflow

private const val SIZE = 1f
private val DOUBLE: Double = 2_000.5
private val INT: Int = 12
private val first = SIZE.dp
private val second = DOUBLE.toFloat().dp
private val third = INT.toInt().sp
class Constants {
    companion object {
        private const val COMPANION = 3F
        val size = COMPANION.sp
    }
}
