package net.chronoflow.app

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import org.junit.Test

class ReleaseAppIdentityTest {
    @Test
    fun releaseBuildHasTheProductionApplicationId() {
        assertThat(BuildConfig.APPLICATION_ID).isEqualTo("net.chronoflow.app.native")
        assertThat(BuildConfig.DEBUG).isFalse()
    }
}
