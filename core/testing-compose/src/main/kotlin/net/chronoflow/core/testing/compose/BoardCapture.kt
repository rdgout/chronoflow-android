package net.chronoflow.core.testing.compose

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onRoot
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue
import java.io.File

/**
 * Writes the root node as a PNG to `<cf.capturesDir>/<name>.png` (Robolectric) or the instrumentation additional output directory (device).
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
        val target = targetFor(name)
        val dialog = rule.onNode(isDialog())
        val hasDialog =
            try {
                dialog.fetchSemanticsNode()
                true
            } catch (_: AssertionError) {
                false
            }
        val bitmap = (if (hasDialog) dialog else rule.onRoot()).captureToImage().asAndroidBitmap()
        write(bitmap, target)
    }

    /** Writes a bitmap drawn outside Compose (the timeline drawing replay) under the same name and filter rules. */
    fun capture(
        bitmap: Bitmap,
        name: String,
    ) = write(bitmap, targetFor(name))

    private fun targetFor(name: String): File {
        require(validName.matches(name) && !name.contains("..")) { "invalid capture name '$name'" }
        val device = Build.FINGERPRINT != "robolectric"
        val arguments = if (device) InstrumentationRegistry.getArguments() else null
        val only =
            (if (device) arguments?.getString("cf.captureOnly") else System.getProperty("cf.captureOnly"))
                .orEmpty()
                .split(',')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        assumeTrue("capture '$name' is not in cf.captureOnly", only.isEmpty() || only.any { name.startsWith(it) })
        val dir =
            if (device) {
                arguments?.getString("additionalTestOutputDir")
                    ?: requireNotNull(
                        InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir("captures"),
                    ).absolutePath
            } else {
                System.getProperty("cf.capturesDir")
                    ?: throw IllegalStateException("cf.capturesDir is not set; run make captures")
            }
        return File(dir, "$name.png")
    }

    private fun write(
        bitmap: Bitmap,
        target: File,
    ) {
        target.parentFile?.mkdirs()
        target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
