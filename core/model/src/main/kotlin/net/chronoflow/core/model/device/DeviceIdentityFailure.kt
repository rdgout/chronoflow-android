package net.chronoflow.core.model.device

sealed class DeviceIdentityFailure : Exception(null, null, false, false) {
    data object InvalidTimestamp : DeviceIdentityFailure()

    data object InvalidRandomness : DeviceIdentityFailure()

    data object InvalidPersistedIdentity : DeviceIdentityFailure()
}
