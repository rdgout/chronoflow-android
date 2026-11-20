package net.chronoflow.feature.onboarding

import net.chronoflow.core.designsystem.layout.BoardMetrics

/** The window-policy column and the TermsUpdate board's policy chevron; OQ-A334 settles progress at 6 dp. */
@BoardMetrics("Walkthrough", "TermsUpdate")
internal object OnboardingMetrics {
    const val WIDE_COLUMN = 560f
    const val POLICY_CHEVRON = 18f
    const val PROGRESS_HEIGHT = 6f
}
