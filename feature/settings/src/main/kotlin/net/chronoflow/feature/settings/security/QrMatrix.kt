package net.chronoflow.feature.settings.security

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

class QrMatrix(
    val size: Int,
    private val bits: BooleanArray,
) {
    operator fun get(
        x: Int,
        y: Int,
    ): Boolean = bits[y * size + x]

    override fun toString(): String = "QrMatrix(size=$size)"

    companion object {
        fun encode(text: String): QrMatrix {
            val matrix =
                QRCodeWriter().encode(
                    text,
                    BarcodeFormat.QR_CODE,
                    0,
                    0,
                    mapOf(
                        EncodeHintType.MARGIN to 0,
                        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                        EncodeHintType.CHARACTER_SET to "UTF-8",
                    ),
                )
            return QrMatrix(
                matrix.width,
                BooleanArray(matrix.width * matrix.height) {
                    matrix[it % matrix.width, it / matrix.width]
                },
            )
        }
    }
}
