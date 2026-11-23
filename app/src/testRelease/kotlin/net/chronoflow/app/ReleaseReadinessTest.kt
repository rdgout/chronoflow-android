package net.chronoflow.app

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import org.junit.Test

class ReleaseReadinessTest {
    private fun missing(name: String) {
        val result = runCatching { Class.forName(name) }
        assertThat(result.exceptionOrNull() ?: IllegalStateException("$name is on the release classpath"))
            .isInstanceOf(ClassNotFoundException::class)
    }

    @Test
    fun releaseShipsNoSeedCode() {
        listOf("SeedReceiverActivity", "TestSeeds", "TestSeed", "UITestTimeline").forEach {
            missing("net.chronoflow.debug.$it")
        }
    }

    @Test
    fun releaseShipsNoPerfCode() {
        missing("net.chronoflow.app.perf.PerfEntry")
        missing("net.chronoflow.app.perf.FixtureTimelineSource")
        assertThat(BuildConfig.PERF_ENTRY).isFalse()
    }

    @Test
    fun releaseShipsNoJankStats() {
        missing("androidx.metrics.performance.JankStats")
        missing("net.chronoflow.app.perf.JankStatsInstaller")
    }

    @Test
    fun releaseIsNotDebuggable() {
        assertThat(BuildConfig.DEBUG).isFalse()
    }

    @Test
    fun releaseShipsNoActivityTestHooksOrRecordingFixtures() {
        val names =
            MainActivity::class.java.declaredFields.map { it.name } +
                MainActivity::class.java.declaredMethods.map { it.name }
        val removedHooks = listOf("fontPreloadOverride", "themeProbe")
        assertThat(names.any { name -> removedHooks.any { name.contains(it, ignoreCase = true) } }).isFalse()
        missing("net.chronoflow.app.RootThemeTest")
        missing("net.chronoflow.app.RootThemeTest\$RecordingApplication")
        missing("net.chronoflow.app.RootThemeTest\$RecordingFontPreloader")
    }
}
