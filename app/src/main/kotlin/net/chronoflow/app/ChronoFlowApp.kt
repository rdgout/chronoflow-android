package net.chronoflow.app

import android.app.Application
import net.chronoflow.app.diagnostics.DebugStrictMode
import net.chronoflow.app.diagnostics.LaunchPath

class ChronoFlowApp : Application() {
    val launchPath = LaunchPath()

    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        if (BuildConfig.DEBUG) DebugStrictMode.install(this, launchPath)
        super.onCreate()
        graph = AppGraph(this)
    }
}
