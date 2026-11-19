package net.chronoflow.feature.trip.actions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.component.CfButton
import net.chronoflow.core.designsystem.component.CfButtonSize
import net.chronoflow.core.designsystem.component.CfButtonStyle
import net.chronoflow.core.designsystem.component.CfDivider
import net.chronoflow.core.designsystem.effect.cfClickable
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.layout.BoardMetrics
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.designsystem.theme.tnum
import net.chronoflow.core.plan.actions.TripAction
import net.chronoflow.core.ui.R

@BoardMetrics("TripActions")
private object TripActionsMetrics {
    val rowIcon = 20.dp
}

@Composable
fun TripActionsContent(
    ui: TripActionsUi,
    onAction: (TripAction) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .testTag("trip.actions")
            .verticalScroll(rememberScrollState())
            .padding(
                start = PlanUITokens.space4.dp,
                end = PlanUITokens.space4.dp,
                top = PlanUITokens.space3.dp,
                bottom = PlanUITokens.space8.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(PlanUITokens.space3.dp),
    ) {
        Column(Modifier.padding(horizontal = PlanUITokens.space1.dp)) {
            Text(
                ui.header.title,
                Modifier.testTag("trip.actions.title").semantics { heading() },
                color = CfTheme.colors.foreground,
                style = CfTheme.type.style(CfTypeScale.screenCardTitle),
            )
            Text(ui.header.subtitle, color = CfTheme.colors.mutedForeground, style = CfTheme.type.screenBody.tnum())
        }
        val shape = RoundedCornerShape(PlanUITokens.radius2Xl.dp)
        Column(Modifier.clip(shape).background(CfTheme.colors.surface2)) {
            TripActionRow(TripAction.Edit, ui.availability.edit) { onAction(TripAction.Edit) }
            CfDivider()
            TripActionRow(TripAction.Duplicate, ui.availability.duplicate) { onAction(TripAction.Duplicate) }
        }
        CfButton(
            stringResource(R.string.plan_cancel),
            onCancel,
            Modifier.fillMaxWidth().testTag("trip.actions.cancel"),
            style = CfButtonStyle.Ghost,
            size = CfButtonSize.Touch,
            enabled = !ui.availability.busy,
            minHeight = AuthTokens.touch.dp,
            textStyle = CfTheme.type.style(CfTypeScale.wizardSecondary),
        )
    }
}

@Composable
private fun TripActionRow(
    action: TripAction,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val edit = action == TripAction.Edit
    val id = if (edit) "edit" else "duplicate"
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = PlanUITokens.space14.dp)
            .alpha(if (enabled) 1f else PlanUITokens.priorityOptionalOpacity)
            .cfClickable(onClick, enabled = enabled)
            .testTag("trip.actions.$id")
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = PlanUITokens.space4.dp, vertical = PlanUITokens.space2.dp),
        horizontalArrangement = Arrangement.spacedBy(PlanUITokens.wizardContentGap.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            (if (edit) LucideGlyphs.PenLine else LucideGlyphs.Copy).imageVector(),
            null,
            Modifier.size(TripActionsMetrics.rowIcon),
            tint = CfTheme.colors.foreground,
        )
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(if (edit) R.string.plan_edit_trip else R.string.plan_duplicate_trip),
                color = CfTheme.colors.foreground,
                style = CfTheme.type.style(CfTypeScale.screenBodyLg).copy(fontWeight = FontWeight.Medium),
            )
            val subtitle =
                if (!enabled) {
                    R.string.plan_needs_a_connection
                } else if (!edit) {
                    R.string.plan_same_flights_and_answers_new_dates
                } else {
                    null
                }
            if (subtitle != null) {
                Text(
                    stringResource(subtitle),
                    Modifier.testTag("trip.actions.$id.subtitle"),
                    color = CfTheme.colors.mutedForeground,
                    style = CfTheme.type.screenBody,
                )
            }
        }
    }
}
