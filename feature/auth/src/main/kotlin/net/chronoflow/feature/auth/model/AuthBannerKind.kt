package net.chronoflow.feature.auth.model

enum class AuthBannerKind {
    Credentials,
    Locked,
    Server,
    EmailTaken,
    Offline,
    ;

    fun title(context: AuthFormContext = AuthFormContext.SignIn): String {
        if (this == Locked && context == AuthFormContext.Forgot) return "Too many requests."
        return when (this) {
            EmailTaken -> "There's already an account with this email."
            Credentials -> "That email and password don't match."
            Locked -> "Too many attempts."
            Server -> "Something went wrong on our side."
            Offline -> "Needs a connection"
        }
    }

    fun body(context: AuthFormContext = AuthFormContext.SignIn): String {
        if (this == Server &&
            context == AuthFormContext.Verify
        ) {
            return "We couldn't send the link. Try again in a moment."
        }
        if (this == Server && context == AuthFormContext.SignUp) return "Your details are kept. Try again in a moment."
        if (this == Server &&
            context == AuthFormContext.Reset
        ) {
            return "Your new password wasn't saved. Try again in a moment."
        }
        if (this == Locked &&
            context == AuthFormContext.Forgot
        ) {
            return "Wait a few minutes before you ask for another link."
        }
        if (this == Locked &&
            (
                context == AuthFormContext.TwoFactor || context == AuthFormContext.Reset ||
                    context == AuthFormContext.SignUp ||
                    context == AuthFormContext.Verify
            )
        ) {
            return "Wait a few minutes before you try again."
        }
        return when (this) {
            EmailTaken -> "Sign in instead, or reset your password."
            Credentials -> "Check both and try again, or reset your password."
            Locked -> "Wait a few minutes before you try again, or reset your password now."
            Server -> "Try again in a moment."
            Offline -> ""
        }
    }
}
