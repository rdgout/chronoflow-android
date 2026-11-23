package net.chronoflow.app

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.withContext

internal fun AppGraph.ownedTestJobs(): List<Job> =
    buildList {
        // The service scopes have independent SupervisorJobs; appScope alone cannot join them.
        for (name in listOf("appScope\$delegate", "controllerLazy", "readinessLazy", "syncLazy")) {
            val lazy =
                AppGraph::class.java.getDeclaredField(name).let { field ->
                    field.isAccessible = true
                    field.get(this@ownedTestJobs) as Lazy<*>
                }
            if (!lazy.isInitialized()) continue
            val owner = checkNotNull(lazy.value)
            val scope = owner as? CoroutineScope ?: ownedTestScope(owner)
            add(checkNotNull(scope.coroutineContext[Job]))
        }
    }

internal suspend fun AppGraph.closeAndJoinForTest() =
    withContext(NonCancellable) {
        val jobs = ownedTestJobs()
        jobs.forEach { it.cancel() }
        close()
        (jobs + ownedTestJobs()).distinct().forEach { it.cancelAndJoin() }
    }

internal fun ownedTestScope(owner: Any): CoroutineScope =
    owner.javaClass.getDeclaredField("scope").let { field ->
        field.isAccessible = true
        field.get(owner) as CoroutineScope
    }
