package net.chronoflow.feature.auth.model.passkey

@JvmInline
value class PasskeyCapability(
    val enabled: Boolean,
) {
    companion object {
        const val SERVER_READY: Boolean = false
        val Shipped: PasskeyCapability = PasskeyCapability(SERVER_READY)
    }
}
