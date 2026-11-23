package net.chronoflow.core.testing.compose

import android.graphics.Typeface
import net.chronoflow.core.testing.junit.measureClassWarmup
import org.robolectric.nativeruntime.DefaultNativeRuntimeLoader

fun warmNativeTextRuntime(
    testClass: Class<*>,
    sdk: Int,
) {
    measureClassWarmup(testClass, sdk, "native-font-runtime") {
        // NATIVE alone leaves Typeface's font-map lock uninitialized in Robolectric's BeforeClass phase.
        DefaultNativeRuntimeLoader.injectAndLoad()
        Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }
}
