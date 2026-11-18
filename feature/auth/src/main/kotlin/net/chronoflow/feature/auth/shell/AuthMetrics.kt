package net.chronoflow.feature.auth.shell

import net.chronoflow.core.designsystem.layout.BoardMetrics

/** AuthSignIn targets and plan.md's Android window policy: a centred column from a 600 dp window. */
@BoardMetrics("AuthSignIn", "AuthStates")
internal object AuthMetrics {
    const val TOUCH = 48
    const val WIDE_BREAKPOINT = 600
    const val WIDE_COLUMN = 560
}
