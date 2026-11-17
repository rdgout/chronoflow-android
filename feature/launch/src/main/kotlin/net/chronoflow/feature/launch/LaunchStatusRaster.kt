package net.chronoflow.feature.launch

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.text.TextPaint
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.res.ResourcesCompat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import net.chronoflow.core.designsystem.R
import net.chronoflow.core.designsystem.generated.TypeMetrics
import kotlin.math.ceil

suspend fun prepareStatusRaster(
    context: Context,
    text: String,
    density: Float,
    fontScale: Float,
    color: Int,
    io: CoroutineDispatcher,
): StatusRaster =
    withContext(io) {
        val height = requireNotNull(TypeMetrics.body.lineHeight) * fontScale
        val paint =
            TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = ResourcesCompat.getFont(context, R.font.figtree_400)
                textSize = TypeMetrics.body.size * fontScale * density
                this.color = color
            }
        val width = ceil(paint.measureText(text).toDouble()) / density
        val ascent = -paint.fontMetrics.ascent / density
        val descent = paint.fontMetrics.descent / density
        val bitmap =
            Bitmap.createBitmap(
                ceil(width * density).toInt().coerceAtLeast(1),
                ceil(height * density).toInt(),
                Bitmap.Config.ARGB_8888,
            )
        Canvas(bitmap).drawText(text, 0f, ((height - ascent - descent) / 2 + ascent) * density, paint)
        StatusRaster(bitmap.asImageBitmap(), width, ascent.toDouble(), descent.toDouble(), height.toDouble())
    }
