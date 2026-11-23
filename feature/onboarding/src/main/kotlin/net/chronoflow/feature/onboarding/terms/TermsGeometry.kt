package net.chronoflow.feature.onboarding.terms

import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.layout.WindowPolicy
import net.chronoflow.feature.onboarding.OnboardingInsets
import net.chronoflow.feature.onboarding.OnboardingMetrics

class TermsGeometry(
    widthDp: Float,
    heightDp: Float,
    insets: OnboardingInsets,
) {
    val top = insets.top + PlanUITokens.space6
    val bottom = insets.bottom + PlanUITokens.space8
    val leading = insets.left + PlanUITokens.space4
    val trailing = insets.right + PlanUITokens.space4
    private val available = maxOf(0f, widthDp - leading - trailing)
    val contentWidth =
        if (widthDp >=
            WindowPolicy.wideBreakpointDp
        ) {
            minOf(available, OnboardingMetrics.WIDE_COLUMN)
        } else {
            available
        }
    val contentLeft = leading + (available - contentWidth) / 2
    val contentTop = top
    val contentHeight = maxOf(0f, heightDp - top - bottom)
}
