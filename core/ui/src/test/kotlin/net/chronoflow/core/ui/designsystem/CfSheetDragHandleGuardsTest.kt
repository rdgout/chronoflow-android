package net.chronoflow.core.ui.designsystem

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.system.CfSheetDragHandle
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.Guards
import net.chronoflow.core.testing.tiers.Layout
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Layout
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h914dp-420dpi")
class CfSheetDragHandleGuardsTest : PrimitiveTestFixture() {
    @Test fun nativeHandleKeepsItsMinimumSlotInsideTheWindow() {
        host { CfSheetDragHandle(Modifier.testTag("sheet.handle")) }
        compose
            .onNodeWithTag("sheet.handle")
            .assertWidthIsAtLeast(PlanUITokens.actionMinHeight.dp)
            .assertHeightIsAtLeast(PlanUITokens.actionMinHeight.dp)
        Guards.assertTouchTargets(compose, DeviceProfiles.pixel8)
        Guards.assertInteractiveInSafeArea(compose, DeviceProfiles.pixel8)
        Guards.assertNoInteractiveOverlap(compose)
        assertTrue(pixels("sheet.handle").hasInk(colors.mutedForeground))
    }
}
