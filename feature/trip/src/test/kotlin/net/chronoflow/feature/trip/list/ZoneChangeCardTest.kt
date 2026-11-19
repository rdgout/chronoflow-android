package net.chronoflow.feature.trip.list

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.trip.page.PageComponentTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ZoneChangeCardTest : PageComponentTest() {
    @Test fun labelAndClickAreOneButton() {
        val row = ListTestRows.zone
        var selected: String? = null
        host { ZoneChangeCard(row, { selected = it }) }
        compose
            .onNodeWithTag("trip.zone.${row.blockId}")
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription))
            .performClick()
        assertEquals(row.blockId, selected)
    }

    @Test fun absentComparisonHasNoPlate() {
        val row = ListTestRows.zone.copy(zoneComparison = null)
        host { ZoneChangeCard(row, {}) }
        compose.onNodeWithTag("trip.zone.${row.blockId}.comparison", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("trip.zone.${row.blockId}.instruction", useUnmergedTree = true).assertExists()
    }
}
