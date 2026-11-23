package net.chronoflow.buildlogic.manifest

object ReleaseManifestAllowlist {
    val EXPORTED = setOf("net.chronoflow.app.MainActivity")
    val PERMISSIONS_REQUIRED =
        setOf(
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.POST_NOTIFICATIONS",
            "android.permission.RECEIVE_BOOT_COMPLETED",
            "android.permission.WAKE_LOCK",
        )
    val PERMISSIONS_OPTIONAL = setOf("DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")
    val GUARD_PERMISSIONS = setOf("android.permission.DUMP")
}
