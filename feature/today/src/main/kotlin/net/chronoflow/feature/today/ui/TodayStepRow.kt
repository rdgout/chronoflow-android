package net.chronoflow.feature.today.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.effect.cfClickable
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.designsystem.theme.tnum
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.ui.R
import net.chronoflow.core.ui.clock.MinuteClock
import net.chronoflow.core.ui.plan.CategorySwatch
import net.chronoflow.core.ui.plan.CategorySwatchSize
import net.chronoflow.feature.today.TodayRowUi
import androidx.compose.ui.semantics.testTag as semanticsTestTag

@Composable
fun TodayStepRow(
    row: TodayRowUi,
    clock: MinuteClock,
    language: Language,
    formatter: CopyLineFormatter,
    onOpenStep: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val todayProbe = LocalTodayProbe.current
    androidx.compose.runtime.SideEffect { todayProbe?.composed(TodayProbe.Kind.ROW, row.blockId) }
    val colors = CfTheme.colors
    val shape = RoundedCornerShape(TodayMetrics.rowRadius)
    val action = { onOpenStep(row.blockId) }
    val markedLabel =
        if (row.mark == null) {
            null
        } else {
            stringResource(
                if (row.mark == CheckStatus.Done) R.string.screens_title_done else R.string.screens_title_skipped,
                row.title,
            )
        }
    val tag = "today.step.${if (row.actionable) "actionable." else ""}${row.blockId}"
    val semantics =
        if (markedLabel == null) {
            Modifier.semantics(mergeDescendants = true) {}
        } else {
            Modifier.clearAndSetSemantics {
                semanticsTestTag = tag
                contentDescription = markedLabel
                role = Role.Button
                onClick {
                    action()
                    true
                }
            }
        }
    Row(
        modifier
            .cfClickable(action, Role.Button, focusShape = shape)
            .then(semantics)
            .testTag(tag)
            .heightIn(min = PlanUITokens.space14.dp)
            .padding(PlanUITokens.space2.dp),
        horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategorySwatch(
            row.type,
            size = CategorySwatchSize.Small,
            modifier =
                Modifier.alpha(
                    if (row.mark ==
                        null
                    ) {
                        1f
                    } else {
                        PlanUITokens.markedOpacity
                    },
                ),
        )
        Column(Modifier.weight(1f)) {
            Text(
                row.title,
                color = if (row.mark == null) colors.foreground else colors.mutedForeground,
                style =
                    CfTheme.type
                        .style(
                            CfTypeScale.screenBodyLg,
                        ).copy(fontWeight = FontWeight.Medium, lineHeight = TodayMetrics.rowTitleLineHeight),
            )
            if (row.mark == null) {
                RowRelativeText(row, clock, language, formatter)
            } else {
                Text(
                    stringResource(
                        if (row.mark ==
                            CheckStatus.Done
                        ) {
                            R.string.screens_done_time_zone
                        } else {
                            R.string.screens_skipped_time_zone
                        },
                        row.markedTimeText,
                        row.zone,
                    ),
                    color = colors.mutedForeground,
                    style = CfTheme.type.screenBody.tnum(),
                )
            }
        }
        if (row.mark != null) {
            val done = row.mark == CheckStatus.Done
            Box(
                Modifier
                    .size((PlanUITokens.space1 * 7).dp)
                    .then(
                        if (done) {
                            Modifier.background(
                                colors.dawn,
                                CircleShape,
                            )
                        } else {
                            Modifier.border(AuthTokens.focusWidth.dp, colors.border, CircleShape)
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (done) {
                        LucideGlyphs.Check.imageVector(
                            TodayMetrics.MARK_STROKE,
                        )
                    } else {
                        LucideGlyphs.SkipForward.imageVector(TodayMetrics.MARK_STROKE)
                    },
                    null,
                    Modifier.size(if (done) TodayMetrics.markGlyphDone else TodayMetrics.markGlyphSkipped),
                    tint = if (done) colors.dawnForeground else colors.mutedForeground,
                )
            }
        }
    }
}
