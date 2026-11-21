package net.chronoflow.core.testing.compose

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithTag
import org.robolectric.shadows.ShadowDialog
import kotlin.math.roundToInt

fun BoardCapture.captureWindow(
    rule: SemanticsNodeInteractionsProvider,
    name: String,
    backgroundTag: String,
) {
    // Robolectric renders windows separately; reproduce native dim and gravity over the activity capture.
    val background = rule.onNodeWithTag(backgroundTag).captureToImage().asAndroidBitmap()
    val foreground = rule.onNode(isDialog()).captureToImage().asAndroidBitmap()
    val params = checkNotNull(ShadowDialog.getLatestDialog().window).attributes
    val bitmap = Bitmap.createBitmap(background.width, background.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawBitmap(background, 0f, 0f, null)
    if (params.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND != 0) {
        canvas.drawARGB((params.dimAmount * 255).roundToInt(), 0, 0, 0)
    }
    val x =
        when (params.gravity and Gravity.HORIZONTAL_GRAVITY_MASK) {
            Gravity.RIGHT -> background.width - foreground.width
            Gravity.CENTER_HORIZONTAL -> (background.width - foreground.width) / 2
            else -> 0
        } + params.x
    val y =
        when (params.gravity and Gravity.VERTICAL_GRAVITY_MASK) {
            Gravity.BOTTOM -> background.height - foreground.height
            Gravity.CENTER_VERTICAL -> (background.height - foreground.height) / 2
            else -> 0
        } + params.y
    canvas.drawBitmap(foreground, x.toFloat(), y.toFloat(), null)
    capture(bitmap, name)
}
