package net.chronoflow.app

import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.platform.app.InstrumentationRegistry
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.tiers.Smoke
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.roundToInt

@Smoke
class PixelProfileInsetsTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private data class Px(
        val top: Int,
        val left: Int,
        val bottom: Int,
        val right: Int,
    )

    @Test
    fun safeDrawingMatchesThePixelProfile() {
        val metrics =
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext.resources.displayMetrics
        assumeTrue(Build.MODEL.startsWith("sdk_gphone") && metrics.widthPixels == 1080 && metrics.densityDpi == 420)
        var captured: Px? = null
        compose.runOnUiThread { compose.activity.enableEdgeToEdge() }
        compose.setContent {
            val density = LocalDensity.current
            val direction = LocalLayoutDirection.current
            val insets = WindowInsets.safeDrawing
            val px =
                Px(
                    insets.getTop(density),
                    insets.getLeft(density, direction),
                    insets.getBottom(density),
                    insets.getRight(density, direction),
                )
            SideEffect {
                captured = px
                Log.i("CfInsets", "top=${px.top} left=${px.left} bottom=${px.bottom} right=${px.right}")
            }
        }
        val root = {
            val raw =
                ViewCompat
                    .getRootWindowInsets(compose.activity.window.decorView)!!
                    .getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            Px(raw.top, raw.left, raw.bottom, raw.right)
        }
        compose.waitUntil(5_000) { captured != null && captured == root() }
        val profile = DeviceProfiles.pixel8
        val expected =
            Px(
                (profile.insets.top * profile.density).roundToInt(),
                (profile.insets.left * profile.density).roundToInt(),
                (profile.insets.bottom * profile.density).roundToInt(),
                (profile.insets.right * profile.density).roundToInt(),
            )
        assertEquals(expected, captured)
    }
}
