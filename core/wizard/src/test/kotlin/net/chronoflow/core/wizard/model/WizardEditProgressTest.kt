package net.chronoflow.core.wizard.model

import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.wizard.generation.TripEditContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class WizardEditProgressTest {
    @Test fun revisitingInputClearsTheEditFloorAndUsesTheStepWeight() =
        runTest(timeout = 60.seconds) {
            val fixture = WizardModelFixture(backgroundScope, testScheduler, ready = true)
            try {
                val model = fixture.model
                model.state = model.state.copy(edit = TripEditContext("trip", "source"), progressFloor = 1.0)
                model.revisit(MemberWizardStep.returnChoice)
                assertNull(model.state.progressFloor)
                assertEquals(0.45, model.progress, 0.0)
                model.go(MemberWizardStep.checking)
                assertEquals(0.60, model.progress, 0.0)
                model.go(MemberWizardStep.ready)
                assertEquals(0.95, model.progress, 0.0)
            } finally {
                fixture.close()
            }
        }

    @Test fun initialEditCheckingFailureAndFirstReadyKeepTheFullFloor() =
        runTest(timeout = 60.seconds) {
            val fixture = WizardModelFixture(backgroundScope, testScheduler)
            try {
                val model = fixture.model
                model.state =
                    model.state.copy(
                        edit = TripEditContext("trip", "source"),
                        step = MemberWizardStep.checking,
                        progressFloor = 1.0,
                    )
                assertEquals(1.0, model.progress, 0.0)
                model.checking = model.checking.copy(failure = DataFailure.Offline)
                assertEquals(1.0, model.progress, 0.0)
                model.go(MemberWizardStep.ready)
                assertEquals(1.0, model.state.progressFloor)
                assertEquals(1.0, model.progress, 0.0)
            } finally {
                fixture.close()
            }
        }
}
