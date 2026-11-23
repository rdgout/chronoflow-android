@file:Suppress("ktlint:compose:compositionlocal-allowlist")

package net.chronoflow.core.ui.plan

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import net.chronoflow.core.designsystem.system.CfSnackbarHost
import net.chronoflow.core.sync.refusal.RefusedChangeModel

val LocalRefusedChanges = staticCompositionLocalOf<RefusedChangeModel?> { null }

object RefusedChangeHosts {
    private val hosts = mutableStateListOf<Any>()

    fun register(token: Any) {
        hosts.add(token)
    }

    fun unregister(token: Any) {
        hosts.remove(token)
    }

    fun isTop(token: Any): Boolean = hosts.lastOrNull() === token
}

@Composable
fun RefusedChangeFeedback(
    modifier: Modifier = Modifier,
    model: RefusedChangeModel? = LocalRefusedChanges.current,
) {
    if (model == null) return
    val token = remember { Any() }
    DisposableEffect(token) {
        RefusedChangeHosts.register(token)
        onDispose { RefusedChangeHosts.unregister(token) }
    }
    val refusal by model.visible.collectAsState()
    val top = RefusedChangeHosts.isTop(token)
    val host = remember { SnackbarHostState() }
    val text = refusal?.notice?.let { refusedChangeText(it) }
    LaunchedEffect(top, refusal?.id) {
        val value = refusal ?: return@LaunchedEffect
        if (top) {
            if (text != null) host.showSnackbar(text, duration = SnackbarDuration.Long)
            model.consume(value.id)
        }
    }
    CfSnackbarHost(host, modifier.testTag("sync.refusal"), onPresented = { RefusedChangeDebug.presented() })
}
