package net.chronoflow.feature.wizard.guest

fun interface GuestSignUpLauncher {
    fun request(onDismissed: () -> Unit)
}
