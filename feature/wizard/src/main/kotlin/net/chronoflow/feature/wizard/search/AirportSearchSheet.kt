@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@file:Suppress("ktlint:standard:max-line-length", "ktlint:compose:modifier-missing-check")

package net.chronoflow.feature.wizard.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.chronoflow.core.designsystem.component.CfButton
import net.chronoflow.core.designsystem.component.CfButtonStyle
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.haptics.HapticEvent
import net.chronoflow.core.designsystem.haptics.LocalHaptics
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.layout.BoardMetrics
import net.chronoflow.core.designsystem.layout.LocalSafeInsets
import net.chronoflow.core.designsystem.system.CfModalBottomSheet
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.ui.R
import net.chronoflow.core.wizard.model.WizardAirportSheet
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.ui.AirportSearchUi
import net.chronoflow.feature.wizard.ui.AirportSheetUi
import net.chronoflow.feature.wizard.ui.WizardCopy
import net.chronoflow.feature.wizard.ui.resolve

@Suppress("ktlint:standard:property-naming")
@BoardMetrics("WizardSheet", "TripSearchStates")
internal object SheetMetrics {
    const val searchStroke = 2.2f
    const val top = 56
    const val gutter = 20
    const val target = 48
    const val headerTitleMax = 220
    const val headerGap = 8
    const val captionGap = 8
    const val fieldGap = 10
    const val field = 40
    const val fieldRadius = 12
    const val fieldInset = 12
    const val fieldIcon = 18
    const val fieldIconGap = 8
    const val row = 60
    const val rowHorizontal = 16
    const val rowVertical = 8
    const val rowGap = 12
    const val codeWidth = 44
    const val radius = 16
    const val statusIcon = 16
    const val statusGap = 8
    const val skeletonHeight = 48
    const val skeletonRadius = 14
    const val skeletonGap = 10
    const val skeletonTop = 16
    const val stateRadius = 22
    const val emptyVertical = 20
    const val emptyHorizontal = 12
    const val errorInset = 16
    const val errorGap = 12
    const val errorIconTop = 2
    const val dash = 6
    const val dashGap = 4
}

@Composable
internal fun AirportSearchSheet(
    ui: AirportSheetUi,
    search: AirportSearchUi,
    retryEnabled: Boolean,
    onEvent: (WizardEvent) -> Unit,
) {
    key(ui.sheet.id) {
        val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val scope = rememberCoroutineScope()
        val events = rememberUpdatedState(onEvent)
        val haptics = LocalHaptics.current
        var selected by remember { mutableStateOf(false) }
        val safe = LocalSafeInsets.current
        val top = with(LocalDensity.current) { safe.getTop(this).toDp() }
        CfModalBottomSheet(
            onDismissRequest = { events.value(WizardEvent.CancelAirport) },
            sheetState = state,
            modifier = Modifier.padding(top = top + SheetMetrics.top.dp).testTag("wizard.airport-search"),
        ) {
            val query = rememberTextFieldState(ui.seedQuery.orEmpty())
            val focus = remember { FocusRequester() }
            val keyboard = LocalSoftwareKeyboardController.current
            LaunchedEffect(ui.sheet.id) {
                snapshotFlow { state.currentValue }.first { it == SheetValue.Expanded }
                focus.requestFocus()
                keyboard?.show()
            }
            LaunchedEffect(query) {
                snapshotFlow { query.text.toString() }.collect { events.value(WizardEvent.Query(it)) }
            }
            Column(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .windowInsetsPadding(
                        safe.only(WindowInsetsSides.Bottom),
                    ).imePadding()
                    .padding(horizontal = SheetMetrics.gutter.dp),
            ) {
                AirportSearchHeader(ui.title.resolve()) {
                    if (!selected) {
                        selected = true
                        scope.launch { state.hide() }.invokeOnCompletion {
                            if (it == null) events.value(WizardEvent.CancelAirport)
                        }
                    }
                }
                ui.caption?.let { caption ->
                    Spacer(Modifier.height(SheetMetrics.captionGap.dp))
                    Text(
                        WizardCopy(
                            "Between {{from}} and {{to}}",
                            mapOf("from" to caption.first, "to" to caption.second),
                        ).resolve(),
                        Modifier.testTag("wizard.airport.caption"),
                        style = CfTheme.type.style(CfTypeScale.wizardHint),
                        color = CfTheme.colors.mutedForeground,
                    )
                }
                Spacer(Modifier.height(SheetMetrics.fieldGap.dp))
                AirportSearchField(query, focus)
                Spacer(Modifier.height(SheetMetrics.fieldGap.dp))
                AirportSearchStates(search, retryEnabled, { events.value(WizardEvent.RetrySearch) }) { airport ->
                    if (!selected) {
                        selected = true
                        haptics.perform(
                            if (ui.sheet.side ==
                                WizardAirportSheet.Side.stop
                            ) {
                                HapticEvent.Light
                            } else {
                                HapticEvent.Selection
                            },
                        )
                        scope.launch { state.hide() }.invokeOnCompletion {
                            if (it == null) events.value(WizardEvent.ChooseAirport(airport, ui.sheet))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AirportSearchHeader(
    title: String,
    onCancel: () -> Unit,
) {
    Layout(
        content = {
            CfButton(
                WizardCopy("Cancel").resolve(),
                onCancel,
                Modifier.testTag("wizard.airport.cancel"),
                style = CfButtonStyle.Ghost,
                textStyle = CfTheme.type.style(CfTypeScale.wizardRow),
                contentColor = CfTheme.colors.dawnText,
                contentPadding = PaddingValues(0.dp),
            )
            Text(
                title,
                Modifier.testTag("wizard.airport.title").semantics { heading() },
                style = CfTheme.type.style(CfTypeScale.wizardSheetTitle),
                color = CfTheme.colors.foreground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier)
        },
        modifier = Modifier.fillMaxWidth().heightIn(min = SheetMetrics.target.dp),
    ) { measurables, constraints ->
        val gap = SheetMetrics.headerGap.dp.roundToPx()
        val cancelMinimum = measurables[0].minIntrinsicWidth(constraints.maxHeight)
        val trailingMinimum = measurables[2].minIntrinsicWidth(constraints.maxHeight)
        val titleWidth =
            minOf(
                measurables[1].maxIntrinsicWidth(constraints.maxHeight),
                SheetMetrics.headerTitleMax.dp.roundToPx(),
                (constraints.maxWidth - 2 * gap - cancelMinimum - trailingMinimum).coerceAtLeast(0),
            )
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val heading = measurables[1].measure(loose.copy(minWidth = titleWidth, maxWidth = titleWidth))
        val remaining = (constraints.maxWidth - heading.width - 2 * gap).coerceAtLeast(0)
        val leadingWidth =
            (remaining / 2)
                .coerceAtLeast(cancelMinimum)
                .coerceAtMost((remaining - trailingMinimum).coerceAtLeast(0))
        val trailingWidth = remaining - leadingWidth
        val cancel = measurables[0].measure(loose.copy(maxWidth = leadingWidth))
        val trailing = measurables[2].measure(loose.copy(maxWidth = trailingWidth))
        val height =
            maxOf(constraints.minHeight, heading.height, cancel.height, trailing.height)
                .coerceAtMost(constraints.maxHeight)
        layout(constraints.maxWidth, height) {
            cancel.placeRelative(0, (height - cancel.height) / 2)
            heading.placeRelative(leadingWidth + gap, (height - heading.height) / 2)
            trailing.placeRelative(constraints.maxWidth - trailingWidth, (height - trailing.height) / 2)
        }
    }
}

@Composable
internal fun AirportSearchField(
    query: TextFieldState,
    focus: FocusRequester,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    BasicTextField(
        state = query,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(
                    SheetMetrics.target.dp,
                ).focusRequester(focus)
                .testTag("wizard.airport.query"),
        textStyle = CfTheme.type.style(CfTypeScale.wizardRow).copy(color = CfTheme.colors.foreground),
        cursorBrush = SolidColor(CfTheme.colors.dawn),
        keyboardOptions =
            KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Search,
            ),
        onKeyboardAction = { keyboard?.hide() },
        lineLimits = TextFieldLineLimits.SingleLine,
        inputTransformation = InputTransformation.maxLength(60),
        decorator = { inner ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(
                            SheetMetrics.field.dp,
                        ).background(
                            CfTheme.colors.wizardPickerFill,
                            RoundedCornerShape(SheetMetrics.fieldRadius.dp),
                        ).padding(horizontal = SheetMetrics.fieldInset.dp)
                        .testTag("wizard.airport.query.box"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SheetMetrics.fieldIconGap.dp),
                ) {
                    Icon(
                        LucideGlyphs.Search.imageVector(SheetMetrics.searchStroke),
                        null,
                        Modifier.size(SheetMetrics.fieldIcon.dp),
                        tint = CfTheme.colors.mutedForeground,
                    )
                    Box(Modifier.weight(1f)) {
                        if (query.text.isEmpty()) {
                            Text(
                                stringResource(R.string.android_wizard_airport_field),
                                style = CfTheme.type.style(CfTypeScale.wizardRow),
                                color = CfTheme.colors.mutedForeground,
                            )
                        }
                        inner()
                    }
                }
            }
        },
    )
}
