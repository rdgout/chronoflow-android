package net.chronoflow.feature.wizard.ready

import androidx.compose.runtime.Composable
import net.chronoflow.feature.wizard.ui.resolve

@Composable
internal fun ReadyText.resolve(): String {
    val resolved = mutableListOf<Pair<String, String>>()
    for ((index, copy) in copies.withIndex()) {
        val arguments =
            copy.arguments.mapValues { (_, value) ->
                resolved.fold(value) { text, (marker, translation) -> text.replace(marker, translation) }
            }
        resolved.add(ReadyText.marker(index) to copy.copy(arguments = arguments).resolve())
    }
    return resolved.fold(pattern) { text, (marker, translation) -> text.replace(marker, translation) }
}
