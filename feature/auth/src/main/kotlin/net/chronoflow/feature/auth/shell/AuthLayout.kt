package net.chronoflow.feature.auth.shell

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import net.chronoflow.core.designsystem.generated.AuthTokens
import kotlin.math.max
import kotlin.math.min

object AuthLayout {
    data class Insets(
        val top: Float,
        val left: Float,
        val bottom: Float,
        val right: Float,
    )

    data class Reveal(
        val offset: Float,
        val fits: Boolean,
    )

    fun content(
        size: Size,
        insets: Insets,
    ): Rect {
        val x = insets.left + AuthTokens.gutter
        val y = insets.top + AuthTokens.gutter
        return Rect(
            x,
            y,
            x + max(0f, size.width - insets.left - insets.right - AuthTokens.gutter * 2),
            y + max(0f, size.height - insets.top - insets.bottom - AuthTokens.gutter - AuthTokens.bottom),
        )
    }

    fun reveal(
        field: Rect,
        submit: Rect,
        viewport: Rect,
    ): Reveal {
        val pair =
            Rect(
                min(field.left, submit.left),
                min(field.top, submit.top),
                max(field.right, submit.right),
                max(field.bottom, submit.bottom),
            )
        val fits = pair.height <= viewport.height
        val offset = max(pair.bottom - viewport.bottom, min(0f, pair.top - viewport.top))
        return Reveal(if (fits) offset else field.top - viewport.top, fits)
    }
}
