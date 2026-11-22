package net.chronoflow.app

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import net.chronoflow.app.diagnostics.DebugStrictMode
import net.chronoflow.app.diagnostics.LaunchPath
import net.chronoflow.feature.launch.LaunchTrace

open class ChronoFlowApp :
    Application(),
    Configuration.Provider,
    net.chronoflow.core.data.reminders.ReminderHost {
    override val reminderRuntime get() = graph.reminderRuntime
    override val workManagerConfiguration: Configuration
        get() =
            Configuration
                .Builder()
                .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.INFO else Log.ERROR)
                .build()

    val launchPath = LaunchPath()

    lateinit var graph: AppGraph
        internal set

    protected open fun createGraph(): AppGraph = AppGraph(this)

    override fun onCreate() {
        if (BuildConfig.DEBUG) DebugStrictMode.install(this, launchPath, LaunchPlatform.ioViolationSink)
        super.onCreate()
        graph = createGraph()
        androidx.lifecycle.ProcessLifecycleOwner
            .get()
            .lifecycle
            .addObserver(graph.activation)
        LaunchTrace.beginLaunch()
    }
}
