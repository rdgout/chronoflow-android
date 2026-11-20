package net.chronoflow.feature.trip.actions

import androidx.annotation.StringRes
import net.chronoflow.core.ui.R

enum class TripActionFailure(
    @get:StringRes val message: Int,
) {
    Offline(R.string.plan_needs_a_connection),
    Duplicate(R.string.plan_we_could_not_duplicate_this_trip_try_again),
    Edit(R.string.plan_something_went_wrong_on_our_side_try_208515),
}

class TripActionFailureEvent(
    val failure: TripActionFailure,
) {
    private val claimed = java.util.concurrent.atomic.AtomicBoolean(false)

    internal fun claim(): Boolean = claimed.compareAndSet(false, true)
}
