package net.chronoflow.feature.launch.welcome

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import net.chronoflow.core.designsystem.generated.TypeMetrics
import net.chronoflow.core.designsystem.generated.WelcomeTokens

object WelcomeMetrics {
    fun fallbackRowHeight(density: Density): Double =
        maxOf(
            WelcomeTokens.moonSize.toDouble(),
            with(density) {
                requireNotNull(TypeMetrics.sectionTitle.lineHeight)
                    .sp
                    .toDp()
                    .value
                    .toDouble()
            },
        )
}
