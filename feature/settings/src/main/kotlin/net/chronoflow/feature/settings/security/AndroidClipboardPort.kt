package net.chronoflow.feature.settings.security

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.os.Build
import android.os.PersistableBundle

class AndroidClipboardPort(
    private val manager: ClipboardManager,
) : ClipboardPort {
    override fun set(
        label: String,
        text: String,
        sensitive: Boolean,
    ): Long {
        val clip = ClipData.newPlainText(label, text)
        if (Build.VERSION.SDK_INT >= 33) {
            clip.description.extras =
                PersistableBundle().apply {
                    putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, sensitive)
                }
        }
        manager.setPrimaryClip(clip)
        return manager.primaryClipDescription?.timestamp ?: clip.description.timestamp
    }

    override fun currentTimestamp(): Long? = manager.primaryClipDescription?.timestamp

    override fun clear() {
        manager.clearPrimaryClip()
    }
}
