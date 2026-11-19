package net.chronoflow.core.plan

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope

fun TestScope.preparer(): PlanPreparer {
    val worker = StandardTestDispatcher(testScheduler)
    return PlanPreparer(PlanDecoder(worker), CopyCatalogProvider(worker), worker) { testPageCopy }
}
