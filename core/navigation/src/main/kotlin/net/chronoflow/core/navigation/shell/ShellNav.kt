package net.chronoflow.core.navigation.shell

import net.chronoflow.core.navigation.Route

object ShellNav {
    fun root(tab: ShellTab): Route =
        when (tab) {
            ShellTab.Today -> Route.Today
            ShellTab.Trips -> Route.Trips
            ShellTab.Settings -> Route.Settings
        }

    fun initial(tab: ShellTab = ShellTab.Today): ShellNavState =
        ShellNavState(tab, ShellTab.entries.associateWith { listOf(root(it)) })

    fun select(
        state: ShellNavState,
        tab: ShellTab,
    ): ShellNavState =
        when {
            tab != state.selected -> state.copy(selected = tab)
            state.stacks.getValue(tab).size > 1 -> state.copy(stacks = state.stacks + (tab to listOf(root(tab))))
            else -> state.copy(scrollToTop = state.scrollToTop + (tab to ((state.scrollToTop[tab] ?: 0) + 1)))
        }

    fun leaveTripPage(
        state: ShellNavState,
        tab: ShellTab,
    ): ShellNavState =
        state.copy(
            selected = tab,
            stacks = state.stacks + (ShellTab.Today to listOf(Route.Today)) + (ShellTab.Trips to listOf(Route.Trips)),
        )

    fun push(
        state: ShellNavState,
        route: Route,
    ): ShellNavState =
        if (route is Route.Wizard) {
            state.copy(overlay = route)
        } else {
            state.copy(stacks = state.stacks + (state.selected to (state.stacks.getValue(state.selected) + route)))
        }

    fun openTrip(
        state: ShellNavState,
        tripId: String,
    ): ShellNavState = push(state, Route.TripPage(tripId))

    fun back(state: ShellNavState): Pair<ShellNavState, Boolean> =
        when {
            state.overlay != null -> {
                state.copy(overlay = null) to true
            }

            state.stacks.getValue(state.selected).size > 1 -> {
                state.copy(
                    stacks = state.stacks + (state.selected to state.stacks.getValue(state.selected).dropLast(1)),
                ) to true
            }

            state.selected != ShellTab.Today -> {
                state.copy(selected = ShellTab.Today) to true
            }

            else -> {
                state to false
            }
        }

    fun wizardSaved(
        state: ShellNavState,
        tripId: String,
    ): ShellNavState =
        state.copy(
            selected = ShellTab.Trips,
            overlay = null,
            stacks =
                state.stacks +
                    (ShellTab.Trips to listOf(Route.Trips, Route.TripPage(tripId, awaitingSync = true))),
        )

    fun fromRoot(route: Route): ShellNavState =
        when (route) {
            Route.Today -> initial(ShellTab.Today)
            Route.Trips -> initial(ShellTab.Trips)
            Route.Settings -> initial(ShellTab.Settings)
            is Route.TripPage -> push(initial(ShellTab.Trips), route)
            is Route.Wizard -> push(initial(ShellTab.Trips), route)
            else -> throw IllegalArgumentException("Not a shell root")
        }

    fun seed(
        state: ShellNavState,
        start: ShellStart,
    ): ShellNavState =
        when (start) {
            is ShellStart.Settings -> {
                state.copy(
                    selected = ShellTab.Settings,
                    stacks = state.stacks + (ShellTab.Settings to (listOf(Route.Settings) + start.path)),
                    overlay = null,
                )
            }

            is ShellStart.Tab -> {
                select(state, start.tab)
            }

            is ShellStart.Trip -> {
                state.copy(
                    selected = ShellTab.Today,
                    stacks = state.stacks + (ShellTab.Today to listOf(Route.Today, Route.TripPage(start.tripId))),
                )
            }
        }
}
