package net.chronoflow.app.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner

@Composable
internal inline fun <reified T : ViewModel> rememberOverlayViewModel(
    key: Any,
    factory: ViewModelProvider.Factory,
    extras: CreationExtras,
): T {
    val owner = checkNotNull(LocalViewModelStoreOwner.current)
    // The outgoing slide still composes after the store has been cleared.
    return remember(owner, key) { ViewModelProvider.create(owner, factory, extras)[T::class.java] }
}
