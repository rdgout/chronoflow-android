package net.chronoflow.core.model.passkey

sealed class PasskeyFailure : Exception(null, null, false, false) {
    data object Canceled : PasskeyFailure()

    data object InvalidEncoding : PasskeyFailure()

    data object Unavailable : PasskeyFailure()

    data object Busy : PasskeyFailure()

    data object AlreadyRegistered : PasskeyFailure()
}
