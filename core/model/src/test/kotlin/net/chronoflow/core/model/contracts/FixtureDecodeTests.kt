package net.chronoflow.core.model.contracts

import net.chronoflow.core.testing.Fixtures
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class FixtureDecodeTests(
    private val path: String,
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun rows(): List<String> = API_FIXTURE_DECODERS.keys.toList()
    }

    @Test
    fun decodes() {
        API_FIXTURE_DECODERS.getValue(path)(Fixtures.bytes(path))
    }
}
