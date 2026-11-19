package net.chronoflow.core.data.passkey

import android.app.Activity
import java.lang.ref.WeakReference

class PasskeyAnchor {
    private var activity = WeakReference<Activity>(null)

    fun attach(activity: Activity) {
        this.activity = WeakReference(activity)
    }

    fun detach(activity: Activity) {
        if (this.activity.get() === activity) this.activity.clear()
    }

    fun current(): Activity? = activity.get()
}
