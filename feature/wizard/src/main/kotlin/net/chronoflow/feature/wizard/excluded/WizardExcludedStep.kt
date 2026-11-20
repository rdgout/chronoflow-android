package net.chronoflow.feature.wizard.excluded

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.feature.wizard.components.rememberServerCopy
import net.chronoflow.feature.wizard.ui.resolve

@Composable
internal fun WizardExcludedStep(
    ui: WizardExcludedUi,
    formatter: CopyLineFormatter?,
) {
    val serverCopy = rememberServerCopy(formatter)
    val shape = RoundedCornerShape(PlanUITokens.radius3Xl.dp)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(PlanUITokens.wizardContentGap.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .testTag("wizard.excluded.reasons")
                .background(CfTheme.colors.wizardNoticeBackground, shape)
                .border(AuthTokens.borderWidth.dp, CfTheme.colors.wizardNoticeBorder, shape)
                .padding(PlanUITokens.space5.dp),
            verticalArrangement = Arrangement.spacedBy(PlanUITokens.wizardChoiceGap.dp),
        ) {
            ui.reasons.forEachIndexed { index, line ->
                Text(
                    serverCopy(line),
                    Modifier.testTag("wizard.excluded.reason.$index"),
                    color = CfTheme.colors.foreground,
                    style = CfTheme.type.style(CfTypeScale.screenBodyLg),
                )
            }
        }
        Text(
            ui.noteCopy.resolve(),
            Modifier.padding(horizontal = PlanUITokens.space1.dp).testTag("wizard.excluded.note"),
            color = CfTheme.colors.mutedForeground,
            style = CfTheme.type.style(CfTypeScale.wizardHint),
        )
    }
}
