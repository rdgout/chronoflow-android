package net.chronoflow.app

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isLessThanOrEqualTo
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.hypot

class LaunchMoonAssetTest {
    private fun check(
        path: String,
        inkRgb: Int,
    ) {
        val image = ImageIO.read(File(path))
        assertThat(image.width).isEqualTo(1152)
        assertThat(image.height).isEqualTo(1152)
        assertThat(image.colorModel.hasAlpha()).isEqualTo(true)
        assertThat(image.getRGB(0, 0) ushr 24).isEqualTo(0)

        val stroke = image.getRGB(576, 420)
        assertThat(stroke ushr 24).isGreaterThan(253)
        for (shift in listOf(16, 8, 0)) {
            val actual = (stroke shr shift) and 0xFF
            val expected = (inkRgb shr shift) and 0xFF
            assertThat(abs(actual - expected)).isLessThanOrEqualTo(2)
        }

        var ink = 0
        var farthest = 0.0
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                if (image.getRGB(x, y) ushr 24 > 0) {
                    ink++
                    farthest = maxOf(farthest, hypot(x - 576.0, y - 576.0))
                }
            }
        }
        assertThat(farthest).isLessThanOrEqualTo(384.0)
        assertThat(ink * 100).isGreaterThan(image.width * image.height - 1)
    }

    @Test
    fun lightMoonIsTheDawnStrokeInsideTheLaunchCircle() =
        check("src/main/res/drawable-xxxhdpi/launch_moon.png", 0xE78A45)

    @Test
    fun nightMoonIsTheDawnStrokeInsideTheLaunchCircle() =
        check("src/main/res/drawable-night-xxxhdpi/launch_moon.png", 0xF2AD73)
}
