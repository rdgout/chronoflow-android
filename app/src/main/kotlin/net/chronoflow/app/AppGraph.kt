package net.chronoflow.app

import android.app.Application
import android.os.Process
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import net.chronoflow.core.data.clock.SystemPlanClock
import net.chronoflow.core.plan.PlanClock
import net.chronoflow.feature.launch.LaunchController

class AppGraph(
    private val app: Application,
    val fontPreloader: FontPreloader = ResourceFontPreloader,
    private val launchProvider: (() -> LaunchController)? = null,
) {
    internal val launchScope by lazy { CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate) }
    internal val processToken = "${Process.myPid()}:${Process.getStartElapsedRealtime()}"
    val launch: LaunchController get() = launchProvider?.invoke() ?: LaunchPlatform.controller(this)
    val planClock: PlanClock by lazy { SystemPlanClock(app) }
}
