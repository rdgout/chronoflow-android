package net.chronoflow.core.testing.compose

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import org.junit.Assume.assumeTrue
import java.io.File

/**
 * Writes the root node as a PNG to `<cf.capturesDir>/<name>.png` (Robolectric; the emulator path arrives in Phase 2).
 *
 * Names follow the board file in `_design/canvas/`: `<Board>[_<state>][_<profile>]_<lang>_<theme>`. Captures that are
 * not boards go under a subdirectory (`harness/`, `timeline/`, `designsystem/`). `cf.captureOnly` is a comma list: a
 * capture runs when any token is a prefix of its name, otherwise the test is skipped. Empty means all.
 */
object BoardCapture {
    private val validName = Regex("^[A-Za-z0-9][A-Za-z0-9._/-]*$")

    fun capture(
        rule: SemanticsNodeInteractionsProvider,
        name: String,
    ) {
        require(validName.matches(name) && !name.contains("..")) { "invalid capture name '$name'" }
        val only =
            System
                .getProperty(
                    "cf.captureOnly",
                ).orEmpty()
                .split(',')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        assumeTrue("capture '$name' is not in cf.captureOnly", only.isEmpty() || only.any { name.startsWith(it) })
        val dir =
            System.getProperty("cf.capturesDir")
                ?: throw IllegalStateException("cf.capturesDir is not set; run make captures")
        val target = File(dir, "$name.png")
        target.parentFile?.mkdirs()
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        target.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }
}
