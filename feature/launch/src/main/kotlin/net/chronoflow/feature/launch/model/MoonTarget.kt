package net.chronoflow.feature.launch.model

import net.chronoflow.core.designsystem.generated.WelcomeTokens
import kotlin.math.max

object MoonTarget {
    data class Insets(
        val top: Double,
        val left: Double,
        val bottom: Double,
        val right: Double,
    )

    fun fallback(
        size: LaunchSize,
        safeAreaInsets: Insets,
        rowHeight: Double,
    ): LaunchRect {
        require(rowHeight.isFinite() && rowHeight > 0)
        val content = contentBounds(size, safeAreaInsets)
        // iOS uses literal 28 and 14; these are the generated moon size and its half.
        val moon = WelcomeTokens.moonSize.toDouble()
        return LaunchRect(content.minX, content.minY + max(moon, rowHeight) / 2 - moon / 2, moon, moon)
    }

    fun contentBounds(
        size: LaunchSize,
        safeAreaInsets: Insets,
    ): LaunchRect {
        require(size.width.isFinite() && size.height.isFinite() && size.width > 0 && size.height > 0)
        val top = max(safeAreaInsets.top + WelcomeTokens.groupGap, WelcomeTokens.topPadding.toDouble())
        val bottom = max(safeAreaInsets.bottom, WelcomeTokens.bottomPadding.toDouble())
        val leading = safeAreaInsets.left + WelcomeTokens.gutter
        val trailing = safeAreaInsets.right + WelcomeTokens.gutter
        return LaunchRect(
            leading,
            top,
            (size.width - leading - trailing).coerceAtLeast(0.0),
            (
                size.height - top -
                    bottom
            ).coerceAtLeast(0.0),
        )
    }
}
