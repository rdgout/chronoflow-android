@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.feature.onboarding.walkthrough

import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.launch.Gate
import net.chronoflow.feature.onboarding.support.OnboardingFixtureTest
import net.chronoflow.feature.onboarding.support.withOnboardingFixture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class WalkthroughSkipTests(
    private val page: WalkthroughModel.Page,
) : OnboardingFixtureTest() {
    @Test fun skipFromEveryPage() =
        runTest(timeout = 60.seconds) {
            withOnboardingFixture { f ->
                val model = f.walkthroughModel()
                if (page.ordinal >= 1) model.advance()
                if (page.ordinal >= 2) model.advance()
                model.skip()
                assertTrue(model.state.value.completed)
                assertEquals(Gate.App, f.gate())
            }
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun pages() = WalkthroughModel.Page.entries.map { arrayOf(it) }
    }
}
