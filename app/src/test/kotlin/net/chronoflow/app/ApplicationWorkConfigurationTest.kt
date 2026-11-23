package net.chronoflow.app

import org.junit.Assert.assertTrue
import org.junit.Test

class ApplicationWorkConfigurationTest {
    @Test fun exposesReminderHost() {
        assertTrue(
            ChronoFlowApp::class.java.interfaces
                .map { it.name }
                .contains("net.chronoflow.core.data.reminders.ReminderHost"),
        )
    }

    @Test fun workManagerInitializesOnlyThroughConfigurationProvider() {
        assertTrue(
            ChronoFlowApp::class.java.interfaces
                .map { it.name }
                .contains("androidx.work.Configuration\$Provider"),
        )
    }
}
