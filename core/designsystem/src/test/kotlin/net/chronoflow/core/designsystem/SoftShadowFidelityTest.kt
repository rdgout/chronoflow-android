package net.chronoflow.core.designsystem

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.effect.skyBackground
import net.chronoflow.core.designsystem.effect.softShadow
import net.chronoflow.core.designsystem.generated.CfDarkColors
import net.chronoflow.core.designsystem.generated.CfLightColors
import net.chronoflow.core.testing.tiers.Layout
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w375dp-h812dp-480dpi")
class SoftShadowFidelityTest(
    private val dark: Boolean,
) {
    @get:Rule val compose = createComposeRule()

    @Test fun cardShadowMatchesBoardInkBelowAndBesideTheCard() {
        var visible by mutableStateOf(true)
        compose.setContent {
            Box(Modifier.fillMaxSize().skyBackground(dark)) {
                val shape = RoundedCornerShape(26.dp)
                if (visible) {
                    Box(
                        Modifier
                            .offset(16.dp, 300.dp)
                            .size(343.dp, 428.dp)
                            .softShadow(shape, dark),
                    )
                }
                Box(
                    Modifier
                        .offset(
                            16.dp,
                            300.dp,
                        ).size(343.dp, 428.dp)
                        .background((if (dark) CfDarkColors else CfLightColors).card, shape),
                )
            }
        }
        val ink = compose.onRoot().captureToImage().asAndroidBitmap()
        compose.runOnIdle { visible = false }
        val plain = compose.onRoot().captureToImage().asAndroidBitmap()
        val means = samples.map { mean(ink, it) }
        val clear = samples.map { mean(plain, it) }
        val expected =
            if (dark) {
                listOf(listOf(106.94, 58.82, 46.92), listOf(35.0, 26.12, 59.03))
            } else {
                listOf(listOf(186.01, 126.20, 110.77), listOf(95.55, 77.76, 125.51))
            }
        val minimumInk = if (dark) listOf(20.0, 2.0) else listOf(8.0, 1.0)
        means.forEachIndexed { i, actual ->
            actual.forEachIndexed { channel, value ->
                assertTrue(
                    "sample $i channel $channel: $value vs ${expected[i][channel]}",
                    kotlin.math.abs(value - expected[i][channel]) <= 4,
                )
            }
            assertTrue("sample $i has insufficient red ink", clear[i][0] - actual[0] >= minimumInk[i])
        }
        val dir = File(System.getProperty("cf.capturesDir"), "designsystem/shadow-oracle")
        dir.mkdirs()
        File(dir, "chosen-${if (dark) "dark" else "light"}.png").outputStream().use {
            ink.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private fun mean(
        image: Bitmap,
        rect: IntArray,
    ): List<Double> {
        val sums = DoubleArray(3)
        for (y in rect[1] until rect[1] + rect[3]) {
            for (x in rect[0] until rect[0] + rect[2]) {
                val p = image.getPixel(x, y)
                sums[0] += p shr 16 and 255
                sums[1] += p shr 8 and 255
                sums[2] += p and 255
            }
        }
        return sums.map { it / (rect[2] * rect[3]) }
    }

    companion object {
        private val samples = listOf(intArrayOf(555, 2188, 11, 31), intArrayOf(1080, 1495, 41, 11))

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "dark={0}")
        fun cases(): Collection<Array<Any>> = listOf(arrayOf(false), arrayOf(true))
    }
}
