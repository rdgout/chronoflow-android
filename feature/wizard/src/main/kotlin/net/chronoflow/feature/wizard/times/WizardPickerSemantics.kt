package net.chronoflow.feature.wizard.times

import androidx.compose.ui.Modifier
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import java.time.LocalDate

val PickerMinimumDate = SemanticsPropertyKey<String>("PickerMinimumDate")
val PickerIs24Hour = SemanticsPropertyKey<Boolean>("PickerIs24Hour")
val PickerLanguage = SemanticsPropertyKey<String>("PickerLanguage")

internal fun Modifier.wizardPickerSemantics(
    minimum: LocalDate?,
    is24Hour: Boolean?,
): Modifier = this then PickerSemanticsElement(minimum, is24Hour)

private data class PickerSemanticsElement(
    val minimum: LocalDate?,
    val is24Hour: Boolean?,
) : ModifierNodeElement<PickerSemanticsNode>() {
    override fun create() = PickerSemanticsNode(minimum, is24Hour)

    override fun update(node: PickerSemanticsNode) {
        node.minimum = minimum
        node.is24Hour = is24Hour
        node.invalidateSemantics()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "wizardPickerSemantics"
    }
}

private class PickerSemanticsNode(
    var minimum: LocalDate?,
    var is24Hour: Boolean?,
) : Modifier.Node(),
    SemanticsModifierNode,
    CompositionLocalConsumerModifierNode {
    override fun SemanticsPropertyReceiver.applySemantics() {
        minimum?.let { this[PickerMinimumDate] = it.toString() }
        is24Hour?.let { this[PickerIs24Hour] = it }
        // Dialog windows have their own composition-local map, which M3 also uses for calendar labels.
        this[PickerLanguage] = currentValueOf(LocalConfiguration).locales[0].language
    }
}
