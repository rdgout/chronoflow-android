package net.chronoflow.core.testing

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isLessThan
import assertk.assertions.startsWith
import org.junit.Test

class DeviceProfilesTest {
    @Test
    fun exactlyTheFiveProfilesInOrder() {
        assertThat(DeviceProfiles.all.map { it.name })
            .isEqualTo(listOf("small", "pixel8", "foldInner", "landscapeSideNav", "largeFont"))
    }

    @Test
    fun pixel8IsTheMeasuredPhone() {
        val pixel8 = DeviceProfiles.pixel8
        assertThat(pixel8.widthDp).isEqualTo(411)
        assertThat(pixel8.heightDp).isEqualTo(914)
        assertThat(pixel8.density).isEqualTo(2.625f)
        assertThat(pixel8.densityDpi).isEqualTo(420)
        assertThat(pixel8.source).startsWith("measured")
    }

    @Test
    fun largeFontOnlyChangesTheFontScale() {
        val large = DeviceProfiles.largeFont
        val pixel8 = DeviceProfiles.pixel8
        assertThat(large.fontScale).isEqualTo(2.0f)
        assertThat(
            large.copy(fontScale = pixel8.fontScale, name = pixel8.name, source = pixel8.source),
        ).isEqualTo(pixel8)
    }

    @Test
    fun smallAndSideNavInsets() {
        assertThat(DeviceProfiles.small.insets).isEqualTo(SafeInsetsDp(24f, 0f, 48f, 0f))
        assertThat(DeviceProfiles.landscapeSideNav.insets.right).isEqualTo(48f)
    }

    @Test
    fun widthsRespectTheBreakpointAndTheMinimum() {
        DeviceProfiles.all.forEach { assertThat(it.widthDp).isGreaterThanOrEqualTo(280) }
        assertThat(DeviceProfiles.foldInner.widthDp).isGreaterThanOrEqualTo(600)
        assertThat(DeviceProfiles.pixel8.widthDp).isLessThan(600)
    }

    @Test
    fun foldInnerCopiesPixel8InsetsAndSaysSo() {
        assertThat(DeviceProfiles.foldInner.insets).isEqualTo(DeviceProfiles.pixel8.insets)
        assertThat(DeviceProfiles.foldInner.source).startsWith("derived")
    }
}
