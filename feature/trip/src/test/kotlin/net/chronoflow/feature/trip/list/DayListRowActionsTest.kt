package net.chronoflow.feature.trip.list

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.onNodeWithTag
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.plan.step.StepActions
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.trip.page.PageComponentTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DayListRowActionsTest : PageComponentTest() {
    private val row = ListTestRows.row
    private val calls = mutableListOf<Pair<String, CheckStatus?>>()

    private fun show(actions: StepActions) {
        host {
            DayListRow(row, null, {}, actions = actions, onCheck = { id, status ->
                calls.add(id to status)
            })
        }
    }

    private fun config() = compose.onNodeWithTag("trip.block.${row.blockId}").fetchSemanticsNode().config

    @Test fun unmarkedActionableOffersMarkAndSkip() {
        show(StepActions.Open)
        val actions = config()[SemanticsActions.CustomActions]
        assertEquals(2, actions.size)
        compose.runOnIdle { actions[1].action() }
        assertEquals(listOf(row.blockId to CheckStatus.Skipped), calls)
    }

    @Test fun markedOffersUndoOnly() {
        show(StepActions.Marked)
        val actions = config()[SemanticsActions.CustomActions]
        assertEquals(1, actions.size)
        compose.runOnIdle { actions.single().action() }
        assertEquals(listOf(row.blockId to null), calls)
        assertNotNull(config().getOrNull(SemanticsProperties.ContentDescription))
        assertNull(config().getOrNull(SemanticsProperties.StateDescription))
    }

    @Test fun informationBlockHasNone() {
        show(StepActions.None)
        assertNull(config().getOrNull(SemanticsActions.CustomActions))
    }
}
