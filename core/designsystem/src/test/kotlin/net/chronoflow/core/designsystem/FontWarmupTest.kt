package net.chronoflow.core.designsystem

import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.designsystem.theme.Figtree
import net.chronoflow.core.designsystem.theme.Fraunces20
import net.chronoflow.core.designsystem.theme.Fraunces32
import net.chronoflow.core.designsystem.theme.Fraunces48
import net.chronoflow.core.designsystem.theme.warmFonts
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.coroutines.ContinuationInterceptor
import kotlin.time.Duration.Companion.seconds

class FontWarmupTest {
    @Test fun fontsPreloadExactlyOnceOnTheGivenDispatcher() =
        runTest(timeout = 60.seconds) {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val loaded = mutableListOf<FontFamily>()
            warmFonts({
                assertEquals(dispatcher, currentCoroutineContext()[ContinuationInterceptor])
                loaded += it
            }, dispatcher)
            assertEquals(listOf(Figtree, Fraunces20, Fraunces32, Fraunces48), loaded)
        }
}
