package net.chronoflow.app.shell

import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras

internal class TabViewModelStoreOwner(
    parent: ViewModelStoreOwner,
) : ViewModelStoreOwner,
    HasDefaultViewModelProviderFactory {
    override val viewModelStore = ViewModelStore()
    override var defaultViewModelProviderFactory: ViewModelProvider.Factory = ViewModelProvider.NewInstanceFactory()
        private set
    override var defaultViewModelCreationExtras: CreationExtras = CreationExtras.Empty
        private set

    init {
        updateParent(parent)
    }

    fun updateParent(parent: ViewModelStoreOwner) {
        defaultViewModelProviderFactory =
            (parent as? HasDefaultViewModelProviderFactory)?.defaultViewModelProviderFactory
                ?: ViewModelProvider.NewInstanceFactory()
        defaultViewModelCreationExtras =
            (parent as? HasDefaultViewModelProviderFactory)?.defaultViewModelCreationExtras ?: CreationExtras.Empty
    }
}
