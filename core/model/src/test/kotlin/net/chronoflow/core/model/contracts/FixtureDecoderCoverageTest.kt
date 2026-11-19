package net.chronoflow.core.model.contracts

import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import net.chronoflow.core.testing.Fixtures
import org.junit.Test

class FixtureDecoderCoverageTest {
    @Test
    fun everyApiFixtureHasADecoder() {
        val api = setOf("account", "auth", "errors", "plans", "trips", "wizard")
        val fixtures = Fixtures.manifest().map { it.resource }.filter { it.substringBefore('/') in api }
        assertThat(API_FIXTURE_DECODERS.keys.toList()).containsExactlyInAnyOrder(*fixtures.toTypedArray())
    }
}
