package net.chronoflow.core.ui.designsystem

import net.chronoflow.core.testing.compose.LayoutCase
import net.chronoflow.core.testing.compose.LayoutMatrix
import net.chronoflow.core.testing.compose.designsystem.GallerySection
import net.chronoflow.core.testing.tiers.Layout
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Exercises CfModalBottomSheet through the shared isolated gallery section. */
@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class CfModalBottomSheetGuardsTest(
    case: LayoutCase,
) : PrimitiveGuard(case, GallerySection.Sheet) {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases(): Collection<Array<Any>> = LayoutMatrix.parameters(LayoutMatrix.wide)
    }
}
