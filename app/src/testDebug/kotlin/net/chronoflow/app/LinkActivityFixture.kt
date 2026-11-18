package net.chronoflow.app

import android.content.Intent
import org.robolectric.Robolectric
import org.robolectric.android.controller.ActivityController

internal inline fun withLinkActivity(
    intent: Intent,
    check: (ActivityController<MainActivity>) -> Unit,
) {
    // ActivityScenario's intent matching cannot track MainActivity after its launch-link scrub.
    val controller =
        Robolectric
            .buildActivity(MainActivity::class.java, intent)
            .create()
            .start()
            .resume()
            .visible()
    try {
        check(controller)
    } finally {
        controller.pause().stop().destroy()
    }
}
