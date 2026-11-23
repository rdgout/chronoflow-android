package net.chronoflow.feature.auth.signup

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import net.chronoflow.feature.auth.model.AuthPolicy

fun interface PolicyOpener {
    fun open(url: String)
}

class CustomTabPolicyOpener(
    private val context: Context,
    private val dark: Boolean,
    private val start: (Intent) -> Unit = context::startActivity,
) : PolicyOpener {
    override fun open(url: String) {
        if (AuthPolicy.entries.none { it.url == url }) return
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
