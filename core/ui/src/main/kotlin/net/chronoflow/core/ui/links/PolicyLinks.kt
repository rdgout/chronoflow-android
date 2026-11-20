package net.chronoflow.core.ui.links

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri

object PolicyLinks {
    const val TERMS = "https://chronoflow.net/terms"
    const val PRIVACY = "https://chronoflow.net/privacy"

    fun open(
        context: Context,
        url: String,
        dark: Boolean,
        start: (Intent) -> Unit = context::startActivity,
    ) {
        val uri = url.toUri()
        val tab =
            CustomTabsIntent
                .Builder()
                .setShowTitle(true)
                .setColorScheme(if (dark) CustomTabsIntent.COLOR_SCHEME_DARK else CustomTabsIntent.COLOR_SCHEME_LIGHT)
                .build()
        tab.intent.data = uri
        try {
            start(tab.intent)
        } catch (_: ActivityNotFoundException) {
            try {
                start(Intent(Intent.ACTION_VIEW, uri))
            } catch (_: ActivityNotFoundException) {
            }
        }
    }
}
