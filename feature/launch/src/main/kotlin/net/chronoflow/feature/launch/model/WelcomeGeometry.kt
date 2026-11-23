package net.chronoflow.feature.launch.model

import net.chronoflow.core.designsystem.generated.WelcomeTokens
import kotlin.math.max

data class WelcomeGroups(
    val heading: LaunchRect,
    val actions: LaunchRect,
)

@ConsistentCopyVisibility
data class WelcomeGeometry private constructor(
    val content: LaunchRect,
    val top: Double,
    val bottom: Double,
    val leading: Double,
    val trailing: Double,
) {
    constructor(size: LaunchSize, insets: MoonTarget.Insets) : this(
        MoonTarget.contentBounds(size, insets),
        max(insets.top + WelcomeTokens.groupGap, WelcomeTokens.topPadding.toDouble()),
        max(insets.bottom, WelcomeTokens.bottomPadding.toDouble()),
        insets.left + WelcomeTokens.gutter,
        insets.right + WelcomeTokens.gutter,
    )

    fun groups(
        headingHeight: Double,
        actionsHeight: Double,
    ): WelcomeGroups {
        val heading = LaunchRect(content.minX, content.minY, content.width, headingHeight)
        val actions =
            LaunchRect(
                content.minX,
                max(heading.maxY + WelcomeTokens.gutter, content.maxY - actionsHeight),
                content.width,
                actionsHeight,
            )
        return WelcomeGroups(heading, actions)
    }
}
