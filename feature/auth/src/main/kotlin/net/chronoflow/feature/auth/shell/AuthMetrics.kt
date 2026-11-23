package net.chronoflow.feature.auth.shell

import net.chronoflow.core.designsystem.layout.BoardMetrics

/** AuthSignIn targets and plan.md's Android window policy: a centred column from a 600 dp window. */
@BoardMetrics("AuthSignIn", "AuthStates", "AuthSignUp", "AuthTwoFactor")
internal object AuthMetrics {
    const val TOGGLE_GLYPH = 18
    const val TOGGLE_GAP = 8
    const val CONFIRMATION_TO_TOGGLE = 4
    const val VERIFY_BUTTON_GAP = 8
    const val DIFFERENT_EMAIL_HORIZONTAL = 12
    const val FAILED_BUTTON_MIN_HEIGHT = 48
    const val TOUCH = 48
    const val WIDE_BREAKPOINT = 600
    const val WIDE_COLUMN = 560
}
