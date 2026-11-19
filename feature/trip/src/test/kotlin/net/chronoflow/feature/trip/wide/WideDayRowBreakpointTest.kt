package net.chronoflow.feature.trip.wide

import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.width
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config

@Logic
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36])
class WideDayRowBreakpointTest(
    private val width: Int,
    private val listWidth: Float?,
) : WideComponentTest() {
    @Test fun breakpointsSwitchTheLayout() {
        val host = start(width)
        val id =
            host.model.prepared.days[0]
                .id
        if (listWidth == null) {
            compose.onNodeWithTag("trip.wide.list.$id").assertDoesNotExist()
            compose.onNodeWithTag("trip.viewToggle").assertExists()
        } else {
            val list = compose.onNodeWithTag("trip.wide.list.$id").getUnclippedBoundsInRoot()
            assertEquals(listWidth, list.width.value, 0.5f / compose.density.density)
            compose.onNodeWithTag("trip.viewToggle").assertDoesNotExist()
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "width={0}")
        fun widths(): List<Array<Any?>> =
            listOf(
                arrayOf(599, null),
                arrayOf(600, 280f),
                arrayOf(899, 280f),
                arrayOf(900, 320f),
            )
    }
}
