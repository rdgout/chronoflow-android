package net.chronoflow.app

import net.chronoflow.core.testing.tiers.Layout
import org.junit.Test
import org.junit.runner.RunWith

@Layout
@RunWith(org.robolectric.RobolectricTestRunner::class)
abstract class TieredAbstractBaseTest {
    @Test fun runs() = Unit
}
