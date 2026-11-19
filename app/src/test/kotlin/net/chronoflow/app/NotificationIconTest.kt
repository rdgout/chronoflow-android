package net.chronoflow.app

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test
import java.io.File

class NotificationIconTest {
    private val root = parseXml("src/main/res/drawable/ic_stat_moon.xml").documentElement
    private val path = root.children("path").single()

    @Test
    fun isA24DpIconOnA24Viewport() {
        assertThat(root.getAttribute("android:width")).isEqualTo("24dp")
        assertThat(root.getAttribute("android:height")).isEqualTo("24dp")
        assertThat(root.getAttribute("android:viewportWidth")).isEqualTo("24")
        assertThat(root.getAttribute("android:viewportHeight")).isEqualTo("24")
    }

    @Test
    fun usesOnlyWhiteAndTransparent() {
        val colors = Regex("#[0-9A-Fa-f]{8}").findAll(File("src/main/res/drawable/ic_stat_moon.xml").readText())
        assertThat(colors.map { it.value.uppercase() }.toSet()).isEqualTo(setOf("#FFFFFFFF", "#00000000"))
        assertThat(path.getAttribute("android:strokeColor")).isEqualTo("#FFFFFFFF")
        assertThat(path.getAttribute("android:fillColor")).isEqualTo("#00000000")
    }

    @Test
    fun pathIsTheBoardMoon() {
        val svg = File("../_design/canvas/icon/layers/moon-white.svg").readText()
        val board = Regex("""<path d="([^"]+)"""").find(svg)!!.groupValues[1]
        assertThat(path.getAttribute("android:pathData")).isEqualTo(board)
    }
}
