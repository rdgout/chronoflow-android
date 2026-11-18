package net.chronoflow.app

import android.app.Application
import net.chronoflow.app.diagnostics.DebugStrictMode
import net.chronoflow.app.diagnostics.LaunchPath
import net.chronoflow.feature.launch.LaunchTrace

open class ChronoFlowApp : Application() {
    val launchPath = LaunchPath()

    lateinit var graph: AppGraph
        private set

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
