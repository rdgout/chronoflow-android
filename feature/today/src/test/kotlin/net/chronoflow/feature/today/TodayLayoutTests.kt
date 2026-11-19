package net.chronoflow.feature.today

import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.tiers.Layout
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config

@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36])
class TodayLayoutTests(
    profile: DeviceProfile,
) : TodayLayoutSupport(profile) {
    @Test fun testActiveAtXXLarge() = checkTodayLayout("active")

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases(): Collection<Array<Any>> =
            listOf(DeviceProfiles.small, DeviceProfiles.pixel8, DeviceProfiles.foldInner).map {
                arrayOf<Any>(it)
            }
    }
}
