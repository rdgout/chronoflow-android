package net.chronoflow.konsist

import assertk.assertFailure
import assertk.assertions.messageContains
import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.container.KoScope
import org.junit.BeforeClass
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Enclosed::class)
class TodayTestBoundaryTest : KonsistTest() {
    class Project : KonsistTest() {
        @Test fun allUnitTestsKeepAppBootsAtApprovedBoundaries() = unitTestsKeepAppBootsAtApprovedBoundaries(scope)

        companion object {
            private lateinit var scope: KoScope

            @BeforeClass @JvmStatic
            fun parseProject() {
                scope = projectScope()
            }
        }
    }

    @RunWith(Parameterized::class)
    class Planted(
        private val path: String,
    ) : KonsistTest() {
        @Test fun rejectsPlantedViolation() {
            val scope = Konsist.scopeFromFile("konsist/src/test/resources/violations/testBoundary/$path")
            val failure = assertFailure { unitTestsKeepAppBootsAtApprovedBoundaries(scope) }
            failure.messageContains("unitTestsKeepAppBootsAtApprovedBoundaries violated")
            failure.messageContains(path.substringAfterLast('/').removeSuffix(".kt"))
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun cases(): List<Array<Any>> =
                listOf(
                    "app/src/testDebug/kotlin/AppGraphCallTest.kt",
                    "feature/today/src/test/kotlin/QualifiedGraphCallTest.kt",
                    "core/data/src/testRelease/kotlin/SessionGraphCallTest.kt",
                    "core/sync/src/testFixtures/kotlin/SessionGraphsCallTest.kt",
                    "app/src/test/kotlin/MainActivityCallTest.kt",
                    "feature/trip/src/testDebug/kotlin/QualifiedActivityCallTest.kt",
                    "core/ui/src/test/kotlin/ActivityScenarioCallTest.kt",
                    "app/src/testRelease/kotlin/QualifiedScenarioCallTest.kt",
                    "feature/auth/src/test/kotlin/RootContentCallTest.kt",
                    "core/testing-compose/src/test/kotlin/QualifiedRootCallTest.kt",
                    "app/src/test/kotlin/AliasedGraphCallTest.kt",
                ).map { arrayOf<Any>(it) }
        }
    }
}
