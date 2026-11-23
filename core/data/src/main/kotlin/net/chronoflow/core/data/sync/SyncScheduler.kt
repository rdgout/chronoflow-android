package net.chronoflow.core.data.sync

import kotlinx.coroutines.delay
import net.chronoflow.core.sync.SyncScheduler
import java.time.Instant

fun SyncScheduler.Companion.system(): SyncScheduler = SyncScheduler({ Instant.now() }, { delay((it * 1000).toLong()) })
