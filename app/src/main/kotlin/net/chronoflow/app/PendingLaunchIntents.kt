package net.chronoflow.app

import android.content.Intent
import net.chronoflow.app.links.AuthLinkIntentPolicy
import net.chronoflow.app.reminders.ReminderTapIntentPolicy
import net.chronoflow.core.navigation.PendingLaunchIntent

class PendingLaunchIntents {
    private val buffer = PendingLaunchIntent.Buffer()
    private val links = linkedSetOf<String>()

    fun capture(
        intent: Intent,
        restored: Boolean,
    ) {
        AuthLinkIntentPolicy.linkToCapture(intent.action, intent.dataString, intent.flags, restored)?.let(::captureLink)
        val reminder =
            ReminderTapIntentPolicy.tapToCapture(
                intent.action,
                intent.flags,
                restored,
                intent::getStringExtra,
            )
        buffer.capture(PendingLaunchIntent(reminder = reminder))
    }

    fun captureLink(url: String) {
        if (!url.startsWith("https:")) return
        links.remove(url)
        links.add(url)
    }

    fun takeLinks(matching: (String) -> Boolean): List<String> {
        val selected = links.filter(matching)
        links.removeAll(selected.toSet())
        return selected
    }

    fun consume(): PendingLaunchIntent? {
        val reminder = buffer.consume()?.reminder
        val captured = links.toSet()
        links.clear()
        return if (captured.isEmpty() && reminder == null) null else PendingLaunchIntent(captured, reminder)
    }
}
