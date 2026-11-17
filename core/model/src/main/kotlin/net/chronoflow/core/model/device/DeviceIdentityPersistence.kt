package net.chronoflow.core.model.device

interface DeviceIdentityPersistence {
    suspend fun readDeviceID(): String?

    suspend fun writeDeviceID(value: String)
}
