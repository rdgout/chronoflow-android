package net.chronoflow.feature.wizard.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.contracts.CopyLine
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.plan.CopyFormat
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.feature.wizard.ui.WizardCopy
import net.chronoflow.feature.wizard.ui.resolve
import net.chronoflow.feature.wizard.ui.wizardCopy

@Composable
internal fun rememberServerCopy(formatter: CopyLineFormatter?): (CopyLine) -> String {
    val language = Language.of(LocalConfiguration.current.locales[0].language)
    return remember(formatter, language) { { line -> serverCopy(line, formatter, language) } }
}

internal fun serverCopy(
    line: CopyLine,
    formatter: CopyLineFormatter?,
    language: Language,
): String = formatter?.format(line, CopyFormat(language)) ?: ""

internal fun fieldCopy(
    message: FieldMessage,
    formatter: CopyLineFormatter?,
    language: Language,
    wizard: (WizardCopy) -> String,
): String =
    if (formatter?.catalog?.contains(message.key) == true) {
        formatter.catalog.text(message.key, language, message.arguments.mapValues { it.value.content })
    } else {
        wizard(message.wizardCopy())
    }

@Composable
internal fun fieldCopy(
    message: FieldMessage,
    formatter: CopyLineFormatter?,
): String {
    val language = Language.of(LocalConfiguration.current.locales[0].language)
    val catalog = formatter?.catalog
    return if (catalog?.contains(message.key) == true) {
        catalog.text(message.key, language, message.arguments.mapValues { it.value.content })
    } else {
        message.wizardCopy().resolve()
    }
}
