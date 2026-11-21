package net.chronoflow.app.settings

import net.chronoflow.core.navigation.Route

internal fun signedOutDestination(
    route: Route?,
    resetting: Boolean,
): Route? =
    if (route == Route.Welcome || route == Route.SignIn || (route is Route.Reset && resetting)) null else Route.SignIn
