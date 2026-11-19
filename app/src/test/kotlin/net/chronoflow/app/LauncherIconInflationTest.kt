package net.chronoflow.app

import android.graphics.drawable.AdaptiveIconDrawable
import androidx.core.content.res.ResourcesCompat
import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LauncherIconInflationTest {
    @Test
    fun launcherIconInflatesToAnAdaptiveIconWithAllLayers() {
        val resources = RuntimeEnvironment.getApplication().resources
        val icon = ResourcesCompat.getDrawable(resources, R.mipmap.ic_launcher, null)
        assertThat(icon).isNotNull().isInstanceOf<AdaptiveIconDrawable>()
        icon as AdaptiveIconDrawable
        assertThat(icon.background).isNotNull()
        assertThat(icon.foreground).isNotNull()
        assertThat(icon.monochrome).isNotNull()
    }
}
