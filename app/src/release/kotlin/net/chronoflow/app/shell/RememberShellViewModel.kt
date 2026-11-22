package net.chronoflow.app.shell

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.navigation.shell.ShellNav

@Composable
internal fun rememberShellViewModel(root: Route): ShellViewModel = viewModel { ShellViewModel(ShellNav.fromRoot(root)) }
