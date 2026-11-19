package net.chronoflow.core.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.effect.cfClickable
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.layout.BoardMetrics
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.model.contracts.Priority
import net.chronoflow.core.plan.priorityKey
import net.chronoflow.core.plan.step.legendDescription
import net.chronoflow.core.plan.step.legendGroups
import net.chronoflow.core.plan.step.priorityDescription
import net.chronoflow.core.plan.stepLabelKey
import net.chronoflow.core.ui.R

@BoardMetrics("PlanLegend")
private object PlanLegendMetrics {
    val listGap = 10.dp
    val rowMinHeight = 40.dp
    val closeVisual = 44.dp
    val closeIcon = 18.dp
}

@Composable
fun LegendSheetContent(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.testTag("legend.sheet")) {
        LegendHeader(
            stringResource(R.string.screens_what_the_steps_mean),
            stringResource(R.string.screens_close),
            onClose,
            "legend.title",
        )
        Column(
            Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = PlanUITokens.space5.dp,
                    end = PlanUITokens.space5.dp,
                    top = PlanUITokens.space1.dp,
                    bottom = PlanUITokens.space8.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(PlanUITokens.space5.dp),
        ) {
            Text(
                stringResource(R.string.screens_each_kind_of_step_has_its_own_ico_47f13c),
                color = CfTheme.colors.mutedForeground,
                style = CfTheme.type.screenBody,
            )
            legendGroups.forEach { (title, types) ->
                Column(verticalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp)) {
                    LegendHeading(title, "legend.group.$title")
                    Column(verticalArrangement = Arrangement.spacedBy(PlanLegendMetrics.listGap)) {
                        types.forEach { type ->
                            Row(
                                Modifier
                                    .heightIn(min = PlanLegendMetrics.rowMinHeight)
                                    .testTag("legend.type.${type.raw}")
                                    .semantics(mergeDescendants = true) {},
                                horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CategorySwatch(type)
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        planScreenText(stepLabelKey(type) ?: "Note"),
                                        color = CfTheme.colors.foreground,
                                        style =
                                            CfTheme.type
                                                .style(CfTypeScale.screenBodyLg)
                                                .copy(fontWeight = FontWeight.Medium),
                                    )
                                    Text(
                                        planScreenText(legendDescription(type)),
                                        color = CfTheme.colors.mutedForeground,
                                        style = CfTheme.type.screenBody,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            LegendHeading("How important", "legend.importance")
            Column(verticalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp)) {
                LegendHeading("Priority", "legend.priority")
                listOf(
                    listOf(
                        Priority.Core,
                        Priority.Recommended,
                    ),
                    listOf(
                        Priority.Optional,
                        Priority.Info,
                    ),
                ).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp)) {
                        pair.forEach { priority ->
                            Row(
                                Modifier.weight(1f).semantics(mergeDescendants = true) {},
                                horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp),
                            ) {
                                PrioritySample(priority)
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        planScreenText(priorityKey(priority)),
                                        color = CfTheme.colors.foreground,
                                        style = CfTheme.type.screenBody.copy(fontWeight = FontWeight.Medium),
                                    )
                                    Text(
                                        planScreenText(priorityDescription(priority)),
                                        color = CfTheme.colors.mutedForeground,
                                        style = CfTheme.type.style(CfTypeScale.screenMeta),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendHeading(
    key: String,
    tag: String,
) {
    val locale = LocalConfiguration.current.locales[0]
    Text(
        planScreenText(key).uppercase(locale),
        Modifier.testTag(tag).semantics { heading() },
        color = CfTheme.colors.mutedForeground,
        style = CfTheme.type.screenLabel,
    )
}

@Composable
internal fun LegendHeader(
    title: String,
    closeLabel: String,
    onClose: () -> Unit,
    titleTag: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().padding(
            start = PlanUITokens.space5.dp,
            top = PlanUITokens.space3.dp,
            end = PlanUITokens.space3.dp,
            bottom = PlanUITokens.space2.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            Modifier.weight(1f).testTag(titleTag).semantics { heading() },
            color = CfTheme.colors.foreground,
            style = CfTheme.type.style(CfTypeScale.screenCardTitle),
        )
        Box(
            Modifier
                .minimumInteractiveComponentSize()
                .size(PlanLegendMetrics.closeVisual)
                .background(CfTheme.colors.surface2, CircleShape)
                .cfClickable(onClose, focusShape = CircleShape)
                .testTag("legend.close"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                LucideGlyphs.X.imageVector(),
                closeLabel,
                Modifier.size(PlanLegendMetrics.closeIcon),
                tint = CfTheme.colors.foreground,
            )
        }
    }
}
