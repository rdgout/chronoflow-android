package net.chronoflow.app.diagnostics

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.Test

class LaunchPathViolationPolicyTest {
    private val ownFrames = listOf("android.os.StrictMode", "net.chronoflow.app.MainActivity")

    @Test
    fun crashesOnLaunchPathIoWithOwnFrame() {
        assertThat(LaunchPathViolationPolicy.shouldCrash(true, true, ownFrames)).isTrue()
    }

    @Test
    fun doesNotCrashAfterLaunchPath() {
        assertThat(LaunchPathViolationPolicy.shouldCrash(false, true, ownFrames)).isFalse()
    }

    @Test
    fun doesNotCrashOnOtherViolations() {
        assertThat(LaunchPathViolationPolicy.shouldCrash(true, false, ownFrames)).isFalse()
    }

    @Test
    fun doesNotCrashOnFrameworkOnlyStacks() {
        assertThat(LaunchPathViolationPolicy.shouldCrash(true, true, listOf("android.os.Foo", "java.io.File"))).isFalse()
    }
}
