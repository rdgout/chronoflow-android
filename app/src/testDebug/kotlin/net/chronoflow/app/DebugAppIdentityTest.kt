package net.chronoflow.app

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import org.junit.Test

class DebugAppIdentityTest {
    @Test
    fun debugBuildHasTheDebugApplicationId() {
        assertThat(BuildConfig.APPLICATION_ID).isEqualTo("net.chronoflow.app.native.debug")
        assertThat(BuildConfig.DEBUG).isTrue()
    }
}
