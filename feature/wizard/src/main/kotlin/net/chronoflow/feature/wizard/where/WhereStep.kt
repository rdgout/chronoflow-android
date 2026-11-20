@file:Suppress("ktlint:standard:max-line-length", "ktlint:compose:modifier-missing-check")

package net.chronoflow.feature.wizard.where

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.effect.cfClickable
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.haptics.HapticEvent
import net.chronoflow.core.designsystem.haptics.LocalHaptics
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.layout.BoardMetrics
import net.chronoflow.core.designsystem.system.CfSegmentedButtons
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.feature.wizard.components.WizardErrorLine
import net.chronoflow.feature.wizard.components.fieldCopy
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.ui.RouteRowUi
import net.chronoflow.feature.wizard.ui.RowHint
import net.chronoflow.feature.wizard.ui.StopQuestionUi
import net.chronoflow.feature.wizard.ui.WhereUi
import net.chronoflow.feature.wizard.ui.WizardCopy
import net.chronoflow.feature.wizard.ui.resolve

@Suppress("ktlint:standard:property-naming")
@BoardMetrics("Wizard")
internal object WhereMetrics {
    const val swapStroke = 2.2f
    const val actionStroke = 2.4f
    const val none = 0
    const val row = 68
    const val rowVertical = 10
    const val rowStart = 16
    const val rowEnd = 12
    const val swapReserve = 64
    const val swap = 40
    const val swapInset = 14
    const val target = 48
    const val remove = 36
    const val removeGlyph = 14
    const val glyph = 16
    const val noticePadding = 14
    const val noticeGap = 10
    const val noticeIcon = 18
    const val noticeRadius = 18
    const val limitOffset = -4
    const val questionTop = 4
    const val questionInset = 4
    const val errorGap = 6
    const val errorOffset = 2
}

@Composable
internal fun WhereStep(
    state: WhereUi,
    onEvent: (WizardEvent) -> Unit,
) = WhereStep(state, null, onEvent)

@Composable
internal fun WhereStep(
    state: WhereUi,
    formatter: CopyLineFormatter?,
    onEvent: (WizardEvent) -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(PlanUITokens.wizardContentGap.dp)) {
        RouteCard(state, onEvent)
        if (state.showNotice) AudienceNotice()
        if (state.same) {
            Row(
                Modifier.testTag("wizard.where.same").semantics {
                    liveRegion = LiveRegionMode.Polite
                },
                horizontalArrangement = Arrangement.spacedBy(WhereMetrics.errorGap.dp),
            ) {
                Icon(
                    LucideGlyphs.CircleAlert.imageVector(),
                    null,
                    Modifier.offset(y = WhereMetrics.errorOffset.dp).size(WhereMetrics.glyph.dp),
                    tint = CfTheme.colors.destructive,
                )
                Text(
                    WizardCopy("From and To are the same airport. Choose where you land.").resolve(),
                    style = CfTheme.type.screenBody,
                    color = CfTheme.colors.destructive,
                )
            }
        }
        state.serverErrors.forEach { (key, message) ->
            WizardErrorLine(fieldCopy(message, formatter), "wizard.route.error.$key")
        }
        state.question?.let { StopQuestion(it, state, onEvent) }
    }
}

@Composable
internal fun RouteCard(
    state: WhereUi,
    onEvent: (WizardEvent) -> Unit,
) {
    val shape = RoundedCornerShape(PlanUITokens.wizardCardRadius.dp)
    Box(Modifier.fillMaxWidth().testTag("wizard.route")) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(
                    shape,
                ).background(CfTheme.colors.card)
                .border(AuthTokens.borderWidth.dp, CfTheme.colors.border, shape),
        ) {
            state.rows.forEachIndexed { index, row ->
                if (index > 0) HorizontalDivider(thickness = AuthTokens.borderWidth.dp, color = CfTheme.colors.border)
                RouteRow(row, state.showSwap, onEvent)
            }
        }
        if (state.showSwap) {
            val haptics = LocalHaptics.current
            RoundAction(
                LucideGlyphs.ArrowDownUp.imageVector(WhereMetrics.swapStroke),
                state.swapDescription.resolve(),
                WhereMetrics.swap,
                WhereMetrics.glyph,
                state.swapEnabled,
                onClick = {
                    haptics.perform(HapticEvent.Light)
                    onEvent(WizardEvent.SwapAirports)
                },
                modifier =
                    Modifier
                        .align(Alignment.CenterEnd)
                        .padding(
                            end =
                                (
                                    WhereMetrics.swapInset -
                                        (WhereMetrics.target - WhereMetrics.swap) / 2
                                ).dp,
                        ).testTag("wizard.swap"),
            )
        }
    }
}

@Composable
internal fun RouteRow(
    row: RouteRowUi,
    reserveSwap: Boolean,
    onEvent: (WizardEvent) -> Unit,
) {
    val label = row.label.copy.resolve()
    val value =
        row.iata?.let { listOfNotNull(it, row.city).joinToString(" · ") } ?: WizardCopy("Choose an airport").resolve()
    val description =
        if (row.locked) {
            row.description.resolve()
        } else {
            label + ", " +
                (row.iata?.let { it + " · " + row.city.orEmpty() } ?: value)
        }
    val c = CfTheme.colors
    val shape =
        RoundedCornerShape(
            bottomStart = (PlanUITokens.wizardCardRadius - AuthTokens.focusWidth / 2).dp,
            bottomEnd =
                (
                    PlanUITokens.wizardCardRadius -
                        AuthTokens.focusWidth / 2
                ).dp,
        )
    Row(
        Modifier.fillMaxWidth().then(
            if (row.invalid) {
                Modifier.drawWithContent {
                    drawContent()
                    val stroke = AuthTokens.focusWidth.dp.toPx()
                    inset(stroke / 2) {
                        drawOutline(
                            shape.createOutline(size, layoutDirection, this),
                            c.destructive,
                            style = Stroke(stroke),
                        )
                    }
                }
            } else {
                Modifier
            },
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .weight(1f)
                .heightIn(min = WhereMetrics.row.dp)
                .testTag(row.tag)
                .cfClickable(
                    enabled = !row.locked,
                    onClick = { row.sheet?.let { onEvent(WizardEvent.OpenAirport(it)) } },
                ).semantics(mergeDescendants = true) {
                    contentDescription = description
                    if (row.locked) disabled()
                }.padding(
                    start = WhereMetrics.rowStart.dp,
                    end = (if (reserveSwap) WhereMetrics.none else WhereMetrics.rowEnd).dp,
                    top = WhereMetrics.rowVertical.dp,
                    bottom = WhereMetrics.rowVertical.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(PlanUITokens.wizardLabelGap.dp)) {
                Text(
                    label,
                    Modifier.testTag("wizard.label.${row.position}"),
                    style = CfTheme.type.style(CfTypeScale.wizardHint),
                    color = c.mutedForeground,
                )
                Text(
                    value,
                    style =
                        CfTheme.type.style(
                            if (row.iata ==
                                null
                            ) {
                                CfTypeScale.wizardAirportPlaceholder
                            } else {
                                CfTypeScale.wizardAirport
                            },
                        ),
                    color =
                        if (row.iata ==
                            null
                        ) {
                            c.mutedForeground
                        } else {
                            c.foreground
                        },
                )
                row.hint?.let { hint ->
                    Text(
                        WizardCopy(
                            if (hint ==
                                RowHint.LastTrip
                            ) {
                                "From your last trip"
                            } else {
                                "Where you landed"
                            },
                        ).resolve(),
                        style = CfTheme.type.style(CfTypeScale.wizardHint),
                        color =
                            if (hint ==
                                RowHint.LastTrip
                            ) {
                                c.dawnText
                            } else {
                                c.mutedForeground
                            },
                    )
                }
            }
            if (row.locked) {
                Icon(
                    LucideGlyphs.Lock.imageVector(),
                    null,
                    Modifier.size(WhereMetrics.glyph.dp),
                    tint = c.mutedForeground,
                )
            }
        }
        if (reserveSwap) Spacer(Modifier.width(WhereMetrics.swapReserve.dp))
        if (row.removable) {
            val haptics = LocalHaptics.current
            RoundAction(
                LucideGlyphs.X.imageVector(WhereMetrics.actionStroke),
                row.removeDescription.resolve(),
                WhereMetrics.remove,
                WhereMetrics.removeGlyph,
                true,
                onClick = {
                    haptics.perform(HapticEvent.Light)
                    onEvent(WizardEvent.RemoveStop(row.routeIndex))
                },
                modifier = Modifier.padding(end = WhereMetrics.rowEnd.dp).testTag("wizard.remove-stop.${row.position}"),
            )
        }
    }
}

@Composable
private fun RoundAction(
    icon: ImageVector,
    description: String,
    visual: Int,
    glyph: Int,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .size(WhereMetrics.target.dp)
            .alpha(if (enabled) 1f else PlanUITokens.wizardEmptyOpacity)
            .cfClickable(
                onClick,
                enabled = enabled,
                focusShape = CircleShape,
            ).semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(visual.dp).background(CfTheme.colors.wizardPickerFill, CircleShape).then(
                if (visual ==
                    WhereMetrics.swap
                ) {
                    Modifier.testTag("wizard.swap.visual")
                } else {
                    Modifier
                },
            ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                null,
                Modifier.size(glyph.dp),
                tint =
                    if (visual ==
                        WhereMetrics.swap
                    ) {
                        CfTheme.colors.foreground
                    } else {
                        CfTheme.colors.mutedForeground
                    },
            )
        }
    }
}

@Composable
internal fun AudienceNotice() {
    val shape = RoundedCornerShape(WhereMetrics.noticeRadius.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .background(
                CfTheme.colors.wizardNoticeBackground,
                shape,
            ).border(
                AuthTokens.borderWidth.dp,
                CfTheme.colors.wizardNoticeBorder,
                shape,
            ).padding(WhereMetrics.noticePadding.dp)
            .testTag("wizard.audienceNotice")
            .semantics(mergeDescendants = true) {
            },
        horizontalArrangement = Arrangement.spacedBy(WhereMetrics.noticeGap.dp),
    ) {
        Icon(
            LucideGlyphs.Info.imageVector(),
            null,
            Modifier.size(WhereMetrics.noticeIcon.dp),
            tint = CfTheme.colors.foreground,
        )
        Column(verticalArrangement = Arrangement.spacedBy(PlanUITokens.wizardLabelGap.dp)) {
            Text(
                WizardCopy(
                    "ChronoFlow is intended for adults with a regular nightly sleep pattern. It does not offer a plan if you have recently done shift work, or do not usually sleep in one regular nightly episode.",
                ).resolve(),
                style = CfTheme.type.screenLabel,
                color = CfTheme.colors.foreground,
            )
            Text(
                WizardCopy(
                    "ChronoFlow is not suitable if you have bipolar disorder or have had a manic or hypomanic episode: sleep loss, bright light and long journeys can trigger mood episodes. Talk to your doctor before you travel.",
                ).resolve(),
                style = CfTheme.type.screenBody,
                color = CfTheme.colors.foreground,
            )
        }
    }
}

@Composable
internal fun StopQuestion(
    question: StopQuestionUi,
    state: WhereUi,
    onEvent: (WizardEvent) -> Unit,
) {
    val description = question.description.resolve()
    Column(
        Modifier.fillMaxWidth().padding(top = WhereMetrics.questionTop.dp).testTag("wizard.route.question"),
        verticalArrangement = Arrangement.spacedBy(PlanUITokens.wizardChoiceGap.dp),
    ) {
        Text(
            description,
            Modifier.padding(horizontal = WhereMetrics.questionInset.dp),
            style = CfTheme.type.style(CfTypeScale.wizardButton),
            color = CfTheme.colors.foreground,
        )
        CfSegmentedButtons(
            listOf(WizardCopy("No, direct").resolve(), WizardCopy("Yes").resolve()),
            if (question.selected ==
                StopQuestionUi.Selection.WithStops
            ) {
                1
            } else {
                0
            },
            { selected ->
                if (selected == 1) {
                    if (question.yesEnabled &&
                        question.selected == StopQuestionUi.Selection.Direct
                    ) {
                        onEvent(WizardEvent.OpenAirport(question.addStopSheet))
                    }
                } else {
                    onEvent(WizardEvent.MakeDirect(state.home))
                }
            },
            Modifier.fillMaxWidth().semantics { contentDescription = description },
            testTag = "wizard.route.stops",
            optionEnabled = { it == 0 || question.yesEnabled },
            optionHaptic = { if (it == 0 && state.rows.size > 2) HapticEvent.Light else HapticEvent.Selection },
        )
        if (state.limitReached) {
            Text(
                WizardCopy("That's the most we can plan: 16 flights.").resolve(),
                Modifier
                    .offset(
                        y = WhereMetrics.limitOffset.dp,
                    ).padding(horizontal = WhereMetrics.questionInset.dp)
                    .testTag("wizard.route.limit"),
                style = CfTheme.type.style(CfTypeScale.wizardHint),
                color = CfTheme.colors.mutedForeground,
            )
        } else if (state.showAddStop) {
            Row(
                Modifier
                    .heightIn(min = WhereMetrics.target.dp)
                    .cfClickable({
                        onEvent(WizardEvent.OpenAirport(question.addStopSheet))
                    })
                    .padding(
                        horizontal = WhereMetrics.questionInset.dp,
                    ).testTag(
                        "wizard.add-stop",
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement =
                    Arrangement.spacedBy(
                        WhereMetrics.errorGap.dp,
                    ),
            ) {
                Icon(
                    LucideGlyphs.Plus.imageVector(WhereMetrics.actionStroke),
                    null,
                    Modifier.size(WhereMetrics.glyph.dp),
                    tint = CfTheme.colors.dawnText,
                )
                Text(
                    WizardCopy("Add another stop").resolve(),
                    style = CfTheme.type.style(CfTypeScale.wizardFieldLabel),
                    color = CfTheme.colors.dawnText,
                )
            }
        }
    }
}
