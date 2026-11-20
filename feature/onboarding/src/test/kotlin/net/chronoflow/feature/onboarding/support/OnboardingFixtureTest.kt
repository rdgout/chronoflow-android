package net.chronoflow.feature.onboarding.support

import kotlinx.coroutines.test.runTest
import org.junit.BeforeClass
import kotlin.time.Duration.Companion.seconds

abstract class OnboardingFixtureTest {
    companion object {
        private val fixtureReady by lazy {
            runTest(timeout = 60.seconds) {
                withOnboardingFixture { fixture ->
                    fixture.session()
                    fixture.controller.signOut(discardPending = true)
                }
            }
        }

        @BeforeClass
        @JvmStatic
        fun warmFixtureOncePerJvm() {
            // First JDBC and mock-seed initialization must not depend on which behaviour test runs first.
            fixtureReady
        }
    }
}
