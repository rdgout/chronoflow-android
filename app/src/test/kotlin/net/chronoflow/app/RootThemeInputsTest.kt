package net.chronoflow.app

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import net.chronoflow.feature.launch.LaunchController
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class RootThemeInputsTest {
    @Test fun graphSuppliesFontDependenciesAndReadyCallback() = runTest(timeout = 60.seconds) {
        val graph = graph()
        val commit = CompletableDeferred<Unit>()
        val inputs = graph.rootThemeInputs(true, commit)
        assertSame(graph.dispatchers.io, inputs.dispatcher)
        assertSame(graph.fontPreloader, inputs.preloader)
        assertSame(commit, inputs.firstCommit)
        assertFalse(graph.launch.fontsReady)
        inputs.onFontsReady()
        assertTrue(graph.launch.fontsReady)
    }

    @Test fun graphPreservesEnabledReduceMotion() = runTest(timeout = 60.seconds) {
        assertTrue(graph().rootThemeInputs(true, CompletableDeferred()).reduceMotion)
    }

    @Test fun graphPreservesDisabledReduceMotion() = runTest(timeout = 60.seconds) {
        assertFalse(graph().rootThemeInputs(false, CompletableDeferred()).reduceMotion)
    }

    private fun TestScope.graph() = object : RootThemeGraph {
        override val fontPreloader = object : FontPreloader {
            override suspend fun preload(
                resolver: androidx.compose.ui.text.font.FontFamily.Resolver,
                family: androidx.compose.ui.text.font.FontFamily,
            ) = Unit
        }
        override val dispatchers = AppDispatchers(
            main = StandardTestDispatcher(testScheduler), io = StandardTestDispatcher(testScheduler),
        )
        override val launch = LaunchController(moon = { error("wiring test loads no images") }, scope = backgroundScope)
    }
}
