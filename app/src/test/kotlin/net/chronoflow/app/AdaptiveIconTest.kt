package net.chronoflow.app

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isLessThanOrEqualTo
import org.junit.Test
import org.w3c.dom.Element
import kotlin.math.hypot

class AdaptiveIconTest {
    private fun Element.attr(name: String): String = getAttribute("android:$name")

    private fun layer(name: String): Element = parseXml("src/main/res/drawable/ic_launcher_$name.xml").documentElement

    @Test
    fun bothLauncherIconsReferenceAllThreeLayers() {
        for (file in listOf("ic_launcher", "ic_launcher_round")) {
            val root = parseXml("src/main/res/mipmap-anydpi/$file.xml").documentElement
            assertThat(root.tagName).isEqualTo("adaptive-icon")
            for (tag in listOf("background", "foreground", "monochrome")) {
                assertThat(root.children(tag).single().attr("drawable")).isEqualTo("@drawable/ic_launcher_$tag")
            }
        }
    }

    @Test
    fun everyLayerIs108DpOn1024Viewport() {
        for (name in listOf("background", "foreground", "monochrome")) {
            val root = layer(name)
            assertThat(root.attr("width")).isEqualTo("108dp")
            assertThat(root.attr("height")).isEqualTo("108dp")
            assertThat(root.attr("viewportWidth")).isEqualTo("1024")
            assertThat(root.attr("viewportHeight")).isEqualTo("1024")
        }
    }

    @Test
    fun monochromeLayerUsesOnlyOneColor() {
        val path =
            layer("monochrome")
                .children("group")
                .single()
                .children("path")
                .single()
        assertThat(path.attr("strokeColor")).isEqualTo("#FFFFFFFF")
        assertThat(path.attr("fillColor")).isEqualTo("#00000000")
        val colors =
            Regex(
                "#[0-9A-Fa-f]{8}",
            ).findAll(java.io.File("src/main/res/drawable/ic_launcher_monochrome.xml").readText())
        assertThat(colors.map { it.value.uppercase() }.toSet()).isEqualTo(setOf("#FFFFFFFF", "#00000000"))
    }

    @Test
    fun strokedMoonStaysInsideTheSafeZone() {
        val group = layer("foreground").children("group").single()
        val scale = group.attr("scaleX").toDouble()
        val tx = group.attr("translateX").toDouble()
        val ty = group.attr("translateY").toDouble()
        val dpPerUnit = 108.0 / 1024
        val farthest =
            listOf(1.0 to 1.0, 1.0 to 23.0, 23.0 to 1.0, 23.0 to 23.0).maxOf { (x, y) ->
                hypot(tx + scale * x - 512, ty + scale * y - 512) * dpPerUnit
            }
        assertThat(farthest).isLessThanOrEqualTo(33.0)
    }
}
