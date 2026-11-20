@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.feature.wizard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.wizard.search.AirportSearchField
import net.chronoflow.feature.wizard.shell.WizardHeader
import net.chronoflow.feature.wizard.ui.AirportSearchUi
import net.chronoflow.feature.wizard.ui.WhereUi
import net.chronoflow.feature.wizard.ui.WizardChromeUi
import net.chronoflow.feature.wizard.where.RouteCard
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class WizardRecompositionTest {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)

    private class Counts(
        var header: Int = 0,
        var route: Int = 0,
    )

    @Composable private fun Header(
        chrome: WizardChromeUi,
        counts: Counts,
    ) {
        SideEffect { counts.header++ }
        WizardHeader(chrome, {})
    }

    @Composable private fun Card(
        where: WhereUi,
        counts: Counts,
    ) {
        SideEffect { counts.route++ }
        RouteCard(where, {})
    }

    @Test fun typingTenCharactersDoesNotRecomposeTheRouteCardOrHeader() {
        val counts = Counts()
        val base = screenState()
        compose.setContent {
            WizardTestHost {
                val query = rememberTextFieldState()
                val ui =
                    base.copy(
                        search = if (query.text.isEmpty()) AirportSearchUi.Hint else AirportSearchUi.Searching,
                    )
                Column {
                    Header(ui.chrome, counts)
                    Card(checkNotNull(ui.where), counts)
                    AirportSearchField(query, remember { FocusRequester() })
                }
            }
        }
        compose.onNodeWithTag("wizard.airport.query").performClick()
        compose.runOnIdle {
            counts.header = 0
            counts.route = 0
        }
        compose.onNodeWithTag("wizard.airport.query").performTextInput("A")
        compose.onNodeWithTag("wizard.airport.query").performTextInput("m")
        compose.onNodeWithTag("wizard.airport.query").performTextInput("s")
        compose.onNodeWithTag("wizard.airport.query").performTextInput("t")
        compose.onNodeWithTag("wizard.airport.query").performTextInput("e")
        compose.onNodeWithTag("wizard.airport.query").performTextInput("r")
        compose.onNodeWithTag("wizard.airport.query").performTextInput("d")
        compose.onNodeWithTag("wizard.airport.query").performTextInput("a")
        compose.onNodeWithTag("wizard.airport.query").performTextInput("m")
        compose.onNodeWithTag("wizard.airport.query").performTextInput("X")
        compose.runOnIdle {
            assertEquals(0, counts.header)
            assertEquals(0, counts.route)
        }
    }
}
