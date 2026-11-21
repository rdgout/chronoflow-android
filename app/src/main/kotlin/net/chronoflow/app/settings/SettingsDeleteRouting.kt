package net.chronoflow.app.settings

import net.chronoflow.core.navigation.Route

internal fun signedOutDestination(
    route: Route?,
    resetting: Boolean,
    destination: Route = Route.SignIn,
): Route? =
    when {
        destination == Route.Welcome -> if (route == Route.Welcome) null else Route.Welcome
        route == Route.Welcome || route == Route.SignIn || (route is Route.Reset && resetting) -> null
        else -> Route.SignIn
    }
