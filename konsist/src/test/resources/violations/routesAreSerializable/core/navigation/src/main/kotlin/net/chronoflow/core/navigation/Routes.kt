package net.chronoflow.core.navigation

import androidx.navigation3.runtime.NavKey

sealed interface Route : NavKey

data object Today : Route
