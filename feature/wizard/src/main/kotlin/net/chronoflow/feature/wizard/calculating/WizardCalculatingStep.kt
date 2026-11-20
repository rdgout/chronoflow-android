package net.chronoflow.feature.wizard.calculating

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.component.CfSpinner
import net.chronoflow.core.designsystem.effect.softShadow
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.designsystem.theme.LocalReduceMotion
import net.chronoflow.feature.wizard.ui.WizardCopy
import net.chronoflow.feature.wizard.ui.resolve

@Composable
internal fun WizardCalculatingStep(ui: CalculatingUi) {
    val colors = CfTheme.colors
    val shape = RoundedCornerShape(PlanUITokens.radius3Xl.dp)
    Column(Modifier.fillMaxWidth().testTag("wizard.calculating")) {
        Column(
            Modifier
                .fillMaxWidth()
                .testTag("wizard.calculating.card")
                .softShadow(shape, colors.isDark)
                .background(colors.card, shape)
                .border(AuthTokens.borderWidth.dp, colors.border, shape)
                .padding(horizontal = PlanUITokens.space5.dp, vertical = PlanUITokens.space8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(PlanUITokens.space4.dp),
        ) {
            Box(
                Modifier
                    .size(
                        PlanUITokens.space16.dp,
                    ).background(colors.dawn.copy(alpha = CalculatingMetrics.discAlpha), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (LocalReduceMotion.current) {
                    Icon(
                        LucideGlyphs.Clock.imageVector(),
                        null,
                        Modifier.size(PlanUITokens.wizardFailureIconSize.dp).testTag("wizard.calculating.static"),
                        tint = colors.dawnText,
                    )
                } else {
                    CfSpinner(
                        Modifier.testTag("wizard.calculating.spinner"),
                        size = CalculatingMetrics.spinnerSize.dp,
                        color = colors.dawnText,
                    )
                }
            }
            Text(
                ui.route + " · " +
                    WizardCopy(
                        "{{count}} days of steps",
                        mapOf("count" to ui.days.toString()),
                    ).resolve(),
                color = colors.mutedForeground,
                style = CfTheme.type.style(CfTypeScale.wizardFailureBody),
                textAlign = TextAlign.Center,
            )
        }
    }
}
