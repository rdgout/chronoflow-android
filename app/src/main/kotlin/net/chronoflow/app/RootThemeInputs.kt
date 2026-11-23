package net.chronoflow.app

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred
import net.chronoflow.feature.launch.LaunchController

interface RootThemeGraph {
    val fontPreloader: FontPreloader
    val dispatchers: AppDispatchers
    val launch: LaunchController
}

internal data class RootThemeInputs(
    val reduceMotion: Boolean,
    val firstCommit: Deferred<Unit>,
    val preloader: FontPreloader,
    val dispatcher: CoroutineDispatcher,
    val onFontsReady: () -> Unit,
)

internal fun RootThemeGraph.rootThemeInputs(
    reduceMotion: Boolean,
    firstCommit: Deferred<Unit>,
) = RootThemeInputs(reduceMotion, firstCommit, fontPreloader, dispatchers.io, launch::markFontsReady)
