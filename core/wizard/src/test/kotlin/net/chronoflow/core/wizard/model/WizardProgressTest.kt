package net.chronoflow.core.wizard.model

import kotlinx.coroutines.test.runTest
import net.chronoflow.core.wizard.generation.TripEditContext
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.seconds

@RunWith(Enclosed::class)
class WizardProgressTest {
    @RunWith(Parameterized::class)
    class Member(
        private val step: MemberWizardStep,
        private val complete: Boolean,
        private val answers: Boolean,
        private val index: Int,
        private val expected: Double,
    ) {
        @Test fun memberValuesMatchTheBoards() =
            runTest(timeout = 60.seconds) {
                val fixture = WizardModelFixture(backgroundScope, testScheduler)
                val model = fixture.model
                if (!complete) model.form = WizardForm()
                model.state =
                    model.state.copy(
                        step = step,
                        checkingAnswers = answers,
                        questions = listOf(wizardPrompt(), wizardPrompt(id = "two")),
                        questionIndex = index,
                    )
                assertEquals(expected, model.progress, 0.000001)
                if (step == MemberWizardStep.calculating) model.state = model.state.copy(step = MemberWizardStep.ready)
                fixture.close()
            }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{0}-complete={1}-answers={2}-index={3}")
            fun cases() =
                listOf(
                    arrayOf<Any>(MemberWizardStep.whereTo, false, false, 0, 0.08),
                    arrayOf<Any>(MemberWizardStep.whereTo, true, false, 0, 0.15),
                    arrayOf<Any>(MemberWizardStep.outbound, false, false, 0, 0.22),
                    arrayOf<Any>(MemberWizardStep.outbound, true, false, 0, 0.30),
                    arrayOf<Any>(MemberWizardStep.returnChoice, true, false, 0, 0.45),
                    arrayOf<Any>(MemberWizardStep.returnRoute, true, false, 0, 0.50),
                    arrayOf<Any>(MemberWizardStep.returnFlight, true, false, 0, 0.55),
                    arrayOf<Any>(MemberWizardStep.sleep, true, false, 0, 0.50),
                    arrayOf<Any>(MemberWizardStep.checking, true, false, 0, 0.60),
                    arrayOf<Any>(MemberWizardStep.checking, true, true, 0, 0.70),
                    arrayOf<Any>(MemberWizardStep.excluded, true, false, 0, 0.60),
                    arrayOf<Any>(MemberWizardStep.excluded, true, true, 0, 0.70),
                    arrayOf<Any>(MemberWizardStep.question, true, false, 0, 0.60),
                    arrayOf<Any>(MemberWizardStep.question, true, false, 1, 0.65),
                    arrayOf<Any>(MemberWizardStep.continuation, true, false, 0, 0.80),
                    arrayOf<Any>(MemberWizardStep.fineTune, true, false, 0, 0.80),
                    arrayOf<Any>(MemberWizardStep.ready, true, false, 0, 0.95),
                    arrayOf<Any>(MemberWizardStep.calculating, true, false, 0, 1.0),
                )
        }
    }

    @RunWith(Parameterized::class)
    class Sleep(
        private val returned: Boolean,
    ) {
        @Test fun sleepIsFiftyWithOrWithoutAReturn() =
            runTest(timeout = 60.seconds) {
                val fixture = WizardModelFixture(backgroundScope, testScheduler)
                if (returned) fixture.model.form = fixture.model.form.addReturn()
                fixture.model.state = fixture.model.state.copy(step = MemberWizardStep.sleep)
                assertEquals(0.50, fixture.model.progress, 0.0)
                fixture.close()
            }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "return={0}")
            fun cases() = listOf(false, true)
        }
    }

    @RunWith(Parameterized::class)
    class Guest(
        private val answers: Boolean,
    ) {
        @Test fun guestExcludedUsesTheBoardValue() =
            runTest(timeout = 60.seconds) {
                val fixture = WizardModelFixture(backgroundScope, testScheduler, guest = true)
                fixture.model.state =
                    fixture.model.state.copy(step = MemberWizardStep.excluded, checkingAnswers = answers)
                assertEquals(if (answers) 0.64 else 0.60, fixture.model.progress, 0.0)
                fixture.close()
            }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "answers={0}")
            fun cases() = listOf(false, true)
        }
    }

    @RunWith(Parameterized::class)
    class Edit(
        private val step: MemberWizardStep,
    ) {
        @Test fun editInitialPathKeepsTheFullFloor() =
            runTest(timeout = 60.seconds) {
                val fixture = WizardModelFixture(backgroundScope, testScheduler)
                fixture.model.state =
                    fixture.model.state.copy(step = step, edit = TripEditContext("trip", "source"), progressFloor = 1.0)
                assertEquals(1.0, fixture.model.progress, 0.0)
                fixture.close()
            }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "step={0}")
            fun cases() = listOf(MemberWizardStep.checking, MemberWizardStep.ready)
        }
    }

    class Floor {
        @Test fun fineTuneOpenedFromReadyStaysAtNinetyFive() =
            runTest(timeout = 60.seconds) {
                val fixture = WizardModelFixture(backgroundScope, testScheduler, ready = true)
                fixture.model.revisit(MemberWizardStep.fineTune)
                assertEquals(0.95, fixture.model.progress, 0.0)
                fixture.close()
            }
    }
}
