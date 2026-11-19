package net.chronoflow.feature.today

import net.chronoflow.core.testing.tiers.Layout
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Layout
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TodaySkippedLayoutTests : TodayLayoutSupport() {
    @Test fun testSkippedAtXXLarge() = checkTodayLayout("skipped")
}
