package net.chronoflow.core.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.component.CfButton
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.ui.R

@Composable
fun StepGoneContent(
    onSeeToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = CfTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(
                start = PlanUITokens.space5.dp,
                end = PlanUITokens.space5.dp,
                top = PlanUITokens.space3.dp,
                bottom = PlanUITokens.space6.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(PlanUITokens.wizardContentGap.dp),
    ) {
        Box(
            Modifier
                .size(PlanUITokens.stepGoneCircleSize.dp)
                .background(c.muted, CircleShape)
                .clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                LucideGlyphs.SearchX.imageVector(),
                null,
                Modifier.size(PlanUITokens.stepGoneIconSize.dp),
                tint = c.dawnText,
            )
        }
        Text(
            stringResource(R.string.screens_this_step_is_no_longer_in_your_plan),
            Modifier.testTag("step.gone").semantics { heading() },
            color = c.foreground,
            style = CfTheme.type.style(CfTypeScale.screenCardTitle),
        )
        Text(
            stringResource(R.string.screens_your_plan_changed_after_you_opene_3344b8),
            color = c.mutedForeground,
            style = CfTheme.type.style(CfTypeScale.screenBodyLg),
        )
        CfButton(
            stringResource(R.string.screens_see_today_s_steps),
            onSeeToday,
            Modifier.testTag("step.seeToday"),
            fillWidth = true,
            minHeight = PlanUITokens.actionMinHeight.dp,
        )
    }
}
