package net.chronoflow.feature.wizard.ready

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.feature.wizard.ui.WizardCopy
import net.chronoflow.feature.wizard.ui.resolve

@Composable
internal fun ReadyPriceBlock(ui: ReadyPriceUi) {
    val colors = CfTheme.colors
    val prefix = if (ui.showLabel) WizardCopy("Price").resolve() + " " else ""
    val value = ui.value.copy.resolve()
    Column(
        Modifier.padding(horizontal = PlanUITokens.space1.dp).testTag("wizard.ready.price"),
        verticalArrangement = Arrangement.spacedBy(PlanUITokens.wizardLabelGap.dp),
    ) {
        Text(
            buildAnnotatedString {
                append(prefix)
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(value) }
            },
            color = colors.foreground,
            style = CfTheme.type.style(CfTypeScale.wizardSubtitle),
        )
        ui.lines.forEach { line ->
            val alert = line is PriceLine.Balance && line.alert
            Text(
                line.copy.resolve(),
                color = if (alert) colors.destructive else colors.mutedForeground,
                style =
                    CfTheme.type
                        .style(
                            CfTypeScale.wizardHint,
                        ).copy(fontWeight = if (alert) FontWeight.SemiBold else FontWeight.Normal),
            )
        }
    }
}
