package net.chronoflow.feature.wizard.guest

interface GuestSignUpHost {
    fun signUpDismissed(
        needsReview: Boolean,
        onGuestRoute: Boolean,
    )

    suspend fun prepareSignIn()

    fun reviewExpired()
}

fun interface GuestSignUpLauncher {
    fun request(host: GuestSignUpHost)
}
