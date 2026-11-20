package net.chronoflow.feature.wizard.ready

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.effect.softShadow
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.feature.wizard.components.rememberServerCopy
import net.chronoflow.feature.wizard.ui.WizardCopy
import net.chronoflow.feature.wizard.ui.resolve

@Composable
internal fun ReadySummaryCard(
    ui: ReadySummaryUi,
    formatter: CopyLineFormatter?,
) {
    val colors = CfTheme.colors
    val shape = RoundedCornerShape(PlanUITokens.radius3Xl.dp)
    val serverCopy = rememberServerCopy(formatter)
    Column(
        Modifier
            .fillMaxWidth()
            .testTag("wizard.ready.summary")
            .softShadow(shape, colors.isDark)
            .background(colors.card, shape)
            .border(AuthTokens.focusWidth.dp, colors.dawn, shape)
            .padding(horizontal = PlanUITokens.wizardReadySummaryPadding.dp, vertical = PlanUITokens.space4.dp),
        verticalArrangement = Arrangement.spacedBy(PlanUITokens.wizardControlGap.dp),
    ) {
        Text(
            WizardCopy("Your plan").resolve().uppercase(LocalConfiguration.current.locales[0]),
            color = colors.dawnText,
            style = CfTheme.type.style(CfTypeScale.screenEyebrow),
        )
        Text(serverCopy(ui.line), color = colors.foreground, style = CfTheme.type.style(CfTypeScale.wizardReadySummary))
        Text(
            ui.dates.resolve(),
            color = colors.mutedForeground,
            style = CfTheme.type.style(CfTypeScale.wizardHint).copy(fontFeatureSettings = "tnum"),
        )
    }
}
