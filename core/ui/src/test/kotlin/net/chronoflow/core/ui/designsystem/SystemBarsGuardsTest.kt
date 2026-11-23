package net.chronoflow.core.ui.designsystem

import net.chronoflow.core.testing.compose.LayoutCase
import net.chronoflow.core.testing.compose.LayoutMatrix
import net.chronoflow.core.testing.compose.designsystem.GallerySection
import net.chronoflow.core.testing.tiers.Layout
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Exercises CfLargeTopAppBar, CfTopAppBar, CfBackButton through the shared isolated gallery section. */
@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class SystemBarsGuardsTest(
    case: LayoutCase,
) : PrimitiveGuard(case, GallerySection.Bars) {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases(): Collection<Array<Any>> = LayoutMatrix.parameters(LayoutMatrix.phone)
    }
}
