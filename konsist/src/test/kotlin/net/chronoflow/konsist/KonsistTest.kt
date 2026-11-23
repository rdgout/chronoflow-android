package net.chronoflow.konsist

import com.lemonappdev.konsist.api.Konsist
import org.junit.BeforeClass

abstract class KonsistTest {
    companion object {
        private val parserReady by lazy {
            check(
                Konsist
                    .scopeFromFile("konsist/src/test/resources/warmup/ParserWarmup.kt")
                    .classes()
                    .single()
                    .name == "ParserWarmup",
            )
        }

        @BeforeClass
        @JvmStatic
        fun warmParserOncePerJvm() {
            // Kotlin PSI bootstrap belongs to the test JVM setup, outside individual rule timings.
            parserReady
        }
    }
}
