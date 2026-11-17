package net.chronoflow.app

import android.app.Application
import net.chronoflow.core.data.clock.SystemPlanClock
import net.chronoflow.core.plan.PlanClock
import net.chronoflow.feature.launch.LaunchController

class AppGraph(
    private val app: Application,
    val fontPreloader: FontPreloader = ResourceFontPreloader,
    private val launchProvider: () -> LaunchController = { LaunchPlatform.controller },
) {
    val launch: LaunchController get() = launchProvider()
    val planClock: PlanClock by lazy { SystemPlanClock(app) }
}
