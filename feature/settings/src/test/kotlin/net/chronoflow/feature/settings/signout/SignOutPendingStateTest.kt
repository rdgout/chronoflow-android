package net.chronoflow.feature.settings.signout

import net.chronoflow.feature.settings.home.PendingText
import net.chronoflow.feature.settings.home.SettingsHomeState
import net.chronoflow.feature.settings.home.check
import net.chronoflow.feature.settings.home.setting
import org.junit.Assert.assertEquals
import org.junit.Test

class SignOutPendingStateTest {
    @Test fun titleCountsDisplayedChanges() {
        val state =
            SettingsHomeState().apply(
                outbox = listOf(check("1"), setting("2", "bodyClock"), setting("3", "bodyClock")),
            )
        assertEquals(3, state.pendingCount)
        assertEquals(2, state.pending.size)
        assertEquals(PendingText.Sleep, state.pending[1].text)
    }
}
