package net.chronoflow.app.audit

import android.view.inspector.WindowInspector
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.platform.app.InstrumentationRegistry
import net.chronoflow.core.testing.compose.BoardCapture
import org.json.JSONObject
import org.junit.Assume.assumeTrue
import org.junit.AssumptionViolatedException
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class DeviceScreenCapture(
    private val rule: ComposeTestRule,
) {
    fun capture(name: String) {
        assumeTrue("capture is outside cf.captureOnly", only.isEmpty() || only.any(name::startsWith))
        rule.awaitHardwareFrames()
        val bitmap = checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        try {
            BoardCapture.capture(bitmap, name)
        } finally {
            bitmap.recycle()
        }
    }

    companion object {
        private val arguments get() = InstrumentationRegistry.getArguments()
        private val only get() =
            arguments
                .getString(
                    "cf.captureOnly",
                ).orEmpty()
                .split(',')
                .map(String::trim)
                .filter(String::isNotEmpty)

        fun requireMaterial() {
            assumeTrue(
                "material captures were not requested",
                only.isEmpty() || only.any { it.startsWith("material") || "material".startsWith(it) },
            )
        }

        internal fun timingFile(): File {
            val directory =
                arguments.getString("additionalTestOutputDir")
                    ?: checkNotNull(
                        InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir("captures"),
                    ).absolutePath
            return File(directory, "material/timings.jsonl")
        }
    }
}

internal fun ComposeTestRule.awaitHardwareFrames() {
    waitForIdle()
    val pending = AtomicInteger()
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    instrumentation.runOnMainSync {
        val windows = WindowInspector.getGlobalWindowViews().filter { it.isShown && it.isAttachedToWindow }
        check(windows.isNotEmpty()) { "no visible capture window" }
        pending.set(windows.size)
        windows.forEach { decor ->
            decor.viewTreeObserver.registerFrameCommitCallback {
                decor.post {
                    decor.viewTreeObserver.registerFrameCommitCallback { pending.decrementAndGet() }
                    decor.postInvalidateOnAnimation()
                }
            }
            decor.postInvalidateOnAnimation()
        }
    }
    waitUntil(5_000) { pending.get() == 0 }
}

internal class DeviceCaptureTimingRule : TestRule {
    override fun apply(
        base: Statement,
        description: Description,
    ): Statement =
        object : Statement() {
            override fun evaluate() {
                val started = System.nanoTime()
                var status = "failed"
                try {
                    base.evaluate()
                    status = "passed"
                } catch (skipped: AssumptionViolatedException) {
                    status = "skipped"
                    throw skipped
                } finally {
                    val seconds = (System.nanoTime() - started) / 1_000_000_000.0
                    val target = DeviceScreenCapture.timingFile()
                    target.parentFile?.mkdirs()
                    target.appendText(
                        JSONObject()
                            .put(
                                "test",
                                description.displayName,
                            ).put("status", if (status == "passed" && seconds >= 10) "overBudget" else status)
                            .put("wallSeconds", seconds)
                            .toString() +
                            "\n",
                    )
                    if (status == "passed") {
                        check(seconds < 10) { "material capture exceeded 10 s: ${description.methodName}" }
                    }
                }
            }
        }
}
