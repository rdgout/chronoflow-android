package net.chronoflow.app.links

object AuthLinkIntentPolicy {
    fun linkToCapture(
        action: String?,
        dataString: String?,
        flags: Int,
        restored: Boolean,
    ): String? =
        dataString?.takeIf {
            !restored && action == "android.intent.action.VIEW" && flags and 0x00100000 == 0 && it.startsWith("https:")
        }
}
