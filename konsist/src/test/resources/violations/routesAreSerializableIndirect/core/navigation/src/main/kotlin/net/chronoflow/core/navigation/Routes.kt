package net.chronoflow.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route : NavKey

@Serializable
sealed interface Intermediate : Route

data object LeakedRoute : Intermediate
