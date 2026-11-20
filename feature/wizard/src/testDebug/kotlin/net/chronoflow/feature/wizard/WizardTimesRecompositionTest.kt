package net.chronoflow.feature.wizard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.v2.createComposeRule
import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.wizard.debug.WizardBoardCaptureSeed
import net.chronoflow.feature.wizard.times.WizardFlightCard
import net.chronoflow.feature.wizard.times.WizardFlightCardUi
import net.chronoflow.feature.wizard.times.WizardTimesUi
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Locale

@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class WizardTimesRecompositionTest {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)

    private class Counts {
        val values = IntArray(3)
    }

    @Composable private fun Card(
        ui: WizardFlightCardUi,
        counts: Counts,
    ) {
        SideEffect { counts.values[ui.index]++ }
        WizardFlightCard(ui, TimeFormat.Value24, remember { BringIntoViewRequester() }, { _, _ -> })
    }

    @Test fun writingOnePillRecomposesOnlyItsCard() {
        val draft = WizardBoardCaptureSeed.draft("WizardOutboundThree", Locale.ENGLISH)
        val state = mutableStateOf(WizardTimesUi.from(draft, false, now = WizardBoardCaptureSeed.now))
        val counts = Counts()
        compose.setContent { WizardTestHost { Column { state.value.cards.forEach { Card(it, counts) } } } }
        compose.runOnIdle {
            counts.values.fill(0)
            val edited = draft.copy(form = draft.form.updateLeg(2) { it.copy(arriveLocal = "2026-10-14T09:15") })
            state.value = WizardTimesUi.from(edited, false, now = WizardBoardCaptureSeed.now)
        }
        compose.runOnIdle { assertEquals(listOf(0, 0, 1), counts.values.toList()) }
    }
}
