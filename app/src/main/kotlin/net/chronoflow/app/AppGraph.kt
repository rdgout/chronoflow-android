package net.chronoflow.app

import android.app.Application
import net.chronoflow.core.data.clock.SystemPlanClock
import net.chronoflow.core.plan.PlanClock

class AppGraph(
    private val app: Application,
    val fontPreloader: FontPreloader = ResourceFontPreloader,
) {
    val planClock: PlanClock by lazy { SystemPlanClock(app) }
}
