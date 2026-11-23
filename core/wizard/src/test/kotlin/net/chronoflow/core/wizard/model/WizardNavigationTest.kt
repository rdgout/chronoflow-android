package net.chronoflow.core.wizard.model

import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.wizard.generation.TripEditContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class WizardNavigationTest {
    @Test fun whereIsAFirstScreen() =
        runTest(timeout = 60.seconds) {
            val fixture = WizardModelFixture(backgroundScope, testScheduler)
            assertTrue(fixture.model.firstScreen)
            assertFalse(fixture.model.showsBack)
            fixture.close()
        }

    @Test fun duplicateTimesIsAFirstScreenAndCannotPop() =
        runTest(timeout = 60.seconds) {
            val fixture = WizardModelFixture(backgroundScope, testScheduler)
            val model = fixture.model
            model.state = model.state.copy(step = MemberWizardStep.outbound, copiedRoute = "Amsterdam → Tokyo")
            assertTrue(model.firstScreen)
            assertFalse(model.showsBack)
            model.popNavigation(emptyList(), model.navigationPath)
            assertEquals(MemberWizardStep.outbound, model.state.step)
            fixture.close()
        }

    @Test fun revisitedTimesIsNotAFirstScreen() =
        runTest(timeout = 60.seconds) {
            val fixture = WizardModelFixture(backgroundScope, testScheduler)
            fixture.model.state =
                fixture.model.state.copy(
                    step = MemberWizardStep.outbound,
                    copiedRoute = "route",
                    revisit = "fingerprint",
                )
            assertFalse(fixture.model.firstScreen)
            assertTrue(fixture.model.showsBack)
            fixture.close()
        }

    @Test fun editReadyAndEditFailureAreFirstScreens() =
        runTest(timeout = 60.seconds) {
            val fixture = WizardModelFixture(backgroundScope, testScheduler)
            val model = fixture.model
            model.state = model.state.copy(edit = TripEditContext("trip", "source"), step = MemberWizardStep.ready)
            assertTrue(model.firstScreen)
            assertFalse(model.showsBack)
            model.state = model.state.copy(step = MemberWizardStep.checking)
            model.checking = model.checking.copy(failure = DataFailure.Offline)
            assertTrue(model.firstScreen)
            assertFalse(model.showsBack)
            model.popNavigation(model.navigationPath.dropLast(1), model.navigationPath)
            assertEquals(MemberWizardStep.checking, model.state.step)
            fixture.close()
        }

    @Test fun checkingAndCalculatingHideBack() =
        runTest(timeout = 60.seconds) {
            val fixture = WizardModelFixture(backgroundScope, testScheduler)
            fixture.model.state = fixture.model.state.copy(step = MemberWizardStep.checking)
            assertFalse(fixture.model.showsBack)
            fixture.model.state = fixture.model.state.copy(step = MemberWizardStep.calculating)
            assertFalse(fixture.model.showsBack)
            fixture.model.state = fixture.model.state.copy(step = MemberWizardStep.ready)
            fixture.close()
        }

    @Test fun memberCheckingFailureShowsBack() =
        runTest(timeout = 60.seconds) {
            val fixture = WizardModelFixture(backgroundScope, testScheduler)
            fixture.model.state = fixture.model.state.copy(step = MemberWizardStep.checking)
            fixture.model.checking = fixture.model.checking.copy(failure = DataFailure.Offline)
            assertFalse(fixture.model.firstScreen)
            assertTrue(fixture.model.showsBack)
            fixture.close()
        }
}
