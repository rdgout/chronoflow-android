package net.chronoflow.app.reminders

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.withContext
import net.chronoflow.feature.reminders.FirstTripRemindersOfferViewModel
import net.chronoflow.feature.reminders.FirstTripRemindersSheet
import net.chronoflow.feature.reminders.ReminderPermissionLauncher
import net.chronoflow.feature.reminders.RemindersDependencies
import net.chronoflow.feature.reminders.RemindersDependenciesKey
import net.chronoflow.feature.reminders.RemindersMainKey
import net.chronoflow.feature.reminders.RemindersViewModel

@Composable
internal fun FirstTripRemindersHost(
    deps: RemindersDependencies,
    main: CoroutineDispatcher,
    tripId: String,
) {
    val extras =
        remember(deps, main) {
            MutableCreationExtras().apply {
                set(RemindersDependenciesKey, deps)
                set(RemindersMainKey, main)
            }
        }
    val offer: FirstTripRemindersOfferViewModel =
        viewModel(
            key = "trip:$tripId:remindersOffer",
            factory = FirstTripRemindersOfferViewModel.Factory,
            extras = extras,
        )
    val model: RemindersViewModel =
        viewModel(key = "trip:$tripId:firstTrip", factory = RemindersViewModel.Factory, extras = extras)
    val showing by offer.showing.collectAsStateWithLifecycle()
    ReminderPermissionLauncher(
        model.permissionLaunches,
        model::onPermissionResult,
        model::acknowledgePermissionLaunch,
        main,
    )
    LaunchedEffect(offer) { offer.start() }
    LaunchedEffect(model, offer, main) {
        withContext(main) {
            model.permissionRequestsFinished.filterNotNull().collect { id ->
                if (!model.failed) offer.dismiss()
                model.acknowledgePermissionFinished(id)
            }
        }
    }
    if (showing) {
        val state by model.state.collectAsStateWithLifecycle()
        DisposableEffect(model) {
            model.start()
            onDispose { model.stop() }
        }
        FirstTripRemindersSheet(state, model::requestPermission, offer::dismiss)
    }
}
