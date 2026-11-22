package net.chronoflow.feature.auth.model

enum class GuestSignupState {
    Ready,
    Failed,
    Retry,
    Review,
    ;

    val titleKey: String? get() =
        when (this) {
            Ready -> null
            Failed -> "We couldn't create your account."
            Retry -> "Your account is ready."
            Review -> "Your trip needs a new check."
        }

    val messageKey: String? get() =
        when (this) {
            Ready -> null
            Failed -> "Try again in a moment."
            Retry -> "Try saving again shortly."
            Review -> "Review your trip again before creating your account."
        }
}
