package net.chronoflow.feature.wizard.checking

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.effect.softShadow
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.feature.wizard.ui.WizardCopy
import net.chronoflow.feature.wizard.ui.resolve

@Composable
internal fun WizardCheckingStep(ui: WizardCheckingUi) {
    Column(Modifier.fillMaxWidth().testTag("wizard.checking")) {
        if (ui.failure != null) WizardCheckFailure(ui) else WizardCheckingCard(ui)
    }
}

@Composable
private fun WizardCheckingCard(ui: WizardCheckingUi) {
    val c = CfTheme.colors
    val shape = RoundedCornerShape(PlanUITokens.radius3Xl.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .softShadow(shape, c.isDark)
            .background(c.card, shape)
            .border(AuthTokens.borderWidth.dp, c.border, shape)
            .testTag("wizard.checking.card")
            .padding(horizontal = PlanUITokens.space5.dp, vertical = PlanUITokens.wizardCheckingPadding.dp),
        verticalArrangement = Arrangement.spacedBy(PlanUITokens.wizardCheckingGap.dp),
    ) {
        WizardCheckingArc(ui.from, ui.to)
        Column(verticalArrangement = Arrangement.spacedBy(PlanUITokens.space3.dp)) {
            ui.lines.forEachIndexed { index, (line, state) ->
                key(index) { WizardCheckLine(line, state, index) }
            }
        }
        if (ui.slow) {
            Text(
                WizardCopy("This is taking longer than usual. You can keep this screen open.").resolve(),
                Modifier.testTag("wizard.checking.slow").semantics { liveRegion = LiveRegionMode.Polite },
                color = c.mutedForeground,
                style = CfTheme.type.style(CfTypeScale.wizardHint),
            )
        }
    }
}
