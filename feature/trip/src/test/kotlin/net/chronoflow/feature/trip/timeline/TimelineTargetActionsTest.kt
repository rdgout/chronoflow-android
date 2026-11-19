package net.chronoflow.feature.trip.timeline

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.plan.step.DayCheckActions
import net.chronoflow.core.plan.step.StepActions
import net.chronoflow.core.testing.tiers.Logic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TimelineTargetActionsTest {
    @get:Rule val compose = createComposeRule()
    private val rig = DayTimelineRig()
    private val id = checkNotNull(rig.blockTargets.first().blockId)
    private val calls = mutableListOf<Pair<String, CheckStatus?>>()
    private val opened = mutableListOf<String>()

    private fun host(action: StepActions) {
        compose.setContent {
            ChronoFlowTheme {
                Box(Modifier.size(411.dp, 800.dp)) {
                    TimelineTargets(
                        remember { mutableStateOf(rig.reference) },
                        rig.day.id,
                        opened::add,
                        actions = DayCheckActions(if (action == StepActions.None) emptyMap() else mapOf(id to action)),
                        onCheck = { block, status -> calls.add(block to status) },
                    )
                }
            }
        }
    }

    private fun actions() =
        compose
            .onNodeWithTag("trip.block.$id")
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsActions.CustomActions)

    @After fun close() {
        rig.close()
    }

    @Test fun unmarkedActionableOffersMarkAndSkip() {
        host(StepActions.Open)
        assertEquals(2, actions()!!.size)
        compose.runOnIdle { actions()!![1].action() }
        assertEquals(listOf(id to CheckStatus.Skipped), calls)
    }

    @Test fun markedOffersUndoOnly() {
        host(StepActions.Marked)
        assertEquals(1, actions()!!.size)
        compose.runOnIdle { actions()!!.single().action() }
        assertEquals(listOf(id to null), calls)
    }

    @Test fun informationBlockHasNone() {
        host(StepActions.None)
        assertNull(actions())
    }

    @Test fun enterKeyOpensBlock() {
        key(Key.Enter)
    }

    @Test fun directionCenterOpensBlock() {
        key(Key.DirectionCenter)
    }

    @Test fun numPadEnterOpensBlock() {
        key(Key.NumPadEnter)
    }

    @Test fun spaceOpensBlock() {
        key(Key.Spacebar)
    }

    private fun key(key: Key) {
        host(StepActions.Open)
        compose.onNodeWithTag("trip.block.$id").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        compose.onNodeWithTag("trip.block.$id").performKeyInput { pressKey(key) }
        assertEquals(listOf(id), opened)
    }

    @Test
    @Config(qualifiers = "nl")
    fun nlLabels() {
        host(StepActions.Open)
        assertEquals(2, actions()!!.size)
    }
}
