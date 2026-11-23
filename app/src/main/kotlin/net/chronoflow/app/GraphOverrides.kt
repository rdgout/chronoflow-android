package net.chronoflow.app

import net.chronoflow.core.datastore.SessionCipher
import java.io.File
import kotlin.time.Duration

data class MockSeedFlags(
    val unverified: Boolean,
    val walkthroughSeen: Boolean,
    val termsPending: Boolean,
)

data class GraphOverrides(
    val storageDir: File? = null,
    val mockDelay: Duration? = null,
    val sessionAlias: String? = null,
    val mockSeed: MockSeedFlags? = null,
    val sessionCipher: SessionCipher? = null,
    val dispatchers: AppDispatchers? = null,
    val mutationsOffline: Boolean = false,
    val reminderPermission: net.chronoflow.app.reminders.ReminderPermissionOverride? = null,
) {
    companion object {
        val None = GraphOverrides()
    }
}
