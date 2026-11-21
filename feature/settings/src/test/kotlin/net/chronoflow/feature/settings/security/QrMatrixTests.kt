package net.chronoflow.feature.settings.security

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QrMatrixTests {
    private val url = "otpauth://totp/ChronoFlow?secret=JBSWY3DPEHPK3PXP&issuer=ChronoFlow"

    @Test fun encodesTheMockOtpauthUrl() {
        val matrix = QrMatrix.encode(url)
        assertTrue(matrix.size >= 21 && (matrix.size - 21) % 4 == 0)
        val again = QrMatrix.encode(url)
        assertArrayEquals(bits(matrix), bits(again))
        val expected =
            BooleanArray(49) { index ->
                val x = index % 7
                val y = index / 7
                x == 0 || y == 0 || x == 6 || y == 6 || (x in 2..4 && y in 2..4)
            }
        assertArrayEquals(expected, finder(matrix, 0, 0))
        assertArrayEquals(expected, finder(matrix, matrix.size - 7, 0))
        assertArrayEquals(expected, finder(matrix, 0, matrix.size - 7))
    }

    @Test fun toStringHidesContent() {
        val text = QrMatrix.encode(url).toString()
        assertFalse(text.contains("JBSWY3DPEHPK3PXP"))
        assertFalse(text.contains("otpauth"))
    }

    private fun bits(matrix: QrMatrix) =
        BooleanArray(matrix.size * matrix.size) {
            matrix[it % matrix.size, it / matrix.size]
        }

    private fun finder(
        matrix: QrMatrix,
        x: Int,
        y: Int,
    ) = BooleanArray(49) {
        matrix[x + it % 7, y + it / 7]
    }
}
