@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.trip.actions

import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.plan.actions.TripAction
import net.chronoflow.core.ui.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class TripActionsFailureMappingTest(
    private val action: TripAction,
    private val offline: Boolean,
    private val expected: TripActionFailure,
    private val copy: Int,
) {
    @Test fun failuresMapToTheIosCopy() =
        runTest(timeout = 60.seconds) {
            withActionsModel(TripWizardPreparer { _, _ -> TripWizardPrepareOutcome.Failed(offline) }) { vm ->
                vm.startAction(action)
                runCurrent()
                assertEquals(expected, vm.actionFailure.value)
                assertEquals(copy, expected.message)
            }
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}-offline-{1}")
        fun cases(): List<Array<Any>> =
            listOf(
                arrayOf(TripAction.Edit, true, TripActionFailure.Offline, R.string.plan_needs_a_connection),
                arrayOf(TripAction.Duplicate, true, TripActionFailure.Offline, R.string.plan_needs_a_connection),
                arrayOf(
                    TripAction.Edit,
                    false,
                    TripActionFailure.Edit,
                    R.string.plan_something_went_wrong_on_our_side_try_208515,
                ),
                arrayOf(
                    TripAction.Duplicate,
                    false,
                    TripActionFailure.Duplicate,
                    R.string.plan_we_could_not_duplicate_this_trip_try_again,
                ),
            )
    }
}
