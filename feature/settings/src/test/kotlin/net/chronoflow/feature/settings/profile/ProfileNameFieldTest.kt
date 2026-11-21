package net.chronoflow.feature.settings.profile

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileNameFieldTest {
    @Test fun consecutivePullsReplaceAnUneditedFieldAndRestoringDoesNotReplayAnOldReplacement() {
        val initial = SettingsProfileState("Initial", "rick@example.com")
        val field = ProfileNameField(initial)
        val latest = ProfileNameReplacement(2, "Intermediate", "Latest")
        field.receive(latest)
        assertEquals("Latest", field.state.text.toString())

        val restored = ProfileNameField(initial.copy(name = "My draft", nameReplacement = latest))
        restored.receive(latest)
        assertEquals("My draft", restored.state.text.toString())
    }
}
