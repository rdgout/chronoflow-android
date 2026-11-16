package net.chronoflow.app.diagnostics

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.Test

class LaunchPathTest {
    @Test
    fun startsActive() {
        assertThat(LaunchPath().isActive).isTrue()
    }

    @Test
    fun endIsIdempotent() {
        val path = LaunchPath()
        path.end()
        path.end()
        assertThat(path.isActive).isFalse()
    }
}
