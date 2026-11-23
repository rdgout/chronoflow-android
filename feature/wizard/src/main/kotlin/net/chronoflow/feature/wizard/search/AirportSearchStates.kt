@file:Suppress("ktlint:standard:max-line-length", "ktlint:compose:modifier-missing-check")

package net.chronoflow.feature.wizard.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.component.CfButton
import net.chronoflow.core.designsystem.component.CfButtonSize
import net.chronoflow.core.designsystem.component.CfButtonStyle
import net.chronoflow.core.designsystem.component.CfSkeleton
import net.chronoflow.core.designsystem.component.CfSpinner
import net.chronoflow.core.designsystem.effect.cfClickable
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.model.contracts.ApiAirportSuggestion
import net.chronoflow.core.ui.R
import net.chronoflow.feature.wizard.ui.AirportRowUi
import net.chronoflow.feature.wizard.ui.AirportSearchUi
import net.chronoflow.feature.wizard.ui.WizardCopy
import net.chronoflow.feature.wizard.ui.airportPlace
import net.chronoflow.feature.wizard.ui.resolve

@Composable
internal fun ColumnScope.AirportSearchStates(
    state: AirportSearchUi,
    retryEnabled: Boolean,
    retry: () -> Unit,
    choose: (ApiAirportSuggestion) -> Unit,
) {
    val name =
        when (state) {
            AirportSearchUi.Hint -> "hint"
            AirportSearchUi.Searching -> "searching"
            is AirportSearchUi.Results -> "results"
            AirportSearchUi.Empty -> "empty"
            is AirportSearchUi.Failed -> "error"
        }
    Column(Modifier.weight(1f).fillMaxWidth().testTag("wizard.airport.state.$name")) {
        if (state is AirportSearchUi.Results) {
            val shape = RoundedCornerShape(SheetMetrics.radius.dp)
            LazyColumn(
                Modifier
                    .weight(
                        1f,
                        fill = false,
                    ).fillMaxWidth()
                    .clip(
                        shape,
                    ).background(
                        CfTheme.colors.background,
                    ).border(AuthTokens.borderWidth.dp, CfTheme.colors.border, shape)
                    .testTag("wizard.airport.results"),
            ) {
                itemsIndexed(state.airports, key = {
                    _,
                    airport,
                    ->
                    airport.iata
                }, contentType = { _, _ -> "airport" }) { index, airport ->
                    if (index >
                        0
                    ) {
                        HorizontalDivider(thickness = AuthTokens.borderWidth.dp, color = CfTheme.colors.border)
                    }
                    AirportResultRow(airport, choose)
                }
            }
        } else {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                when (state) {
                    AirportSearchUi.Hint -> {
                        Text(
                            stringResource(R.string.android_wizard_search_hint),
                            Modifier.testTag("wizard.airport.hint"),
                            style = CfTheme.type.style(CfTypeScale.wizardHint),
                            color = CfTheme.colors.mutedForeground,
                        )
                    }

                    AirportSearchUi.Searching -> {
                        Column(Modifier.testTag("wizard.airport.searching")) {
                            Row(
                                Modifier.semantics {
                                    liveRegion = LiveRegionMode.Polite
                                },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement =
                                    Arrangement.spacedBy(
                                        SheetMetrics.statusGap.dp,
                                    ),
                            ) {
                                CfSpinner(size = SheetMetrics.statusIcon.dp, color = CfTheme.colors.mutedForeground)
                                Text(
                                    stringResource(R.string.android_wizard_search_searching),
                                    style = CfTheme.type.screenBody,
                                    color = CfTheme.colors.mutedForeground,
                                )
                            }
                            Spacer(Modifier.height(SheetMetrics.skeletonTop.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(SheetMetrics.skeletonGap.dp)) {
                                CfSkeleton(
                                    Modifier.fillMaxWidth().height(SheetMetrics.skeletonHeight.dp),
                                    RoundedCornerShape(SheetMetrics.skeletonRadius.dp),
                                )
                                CfSkeleton(
                                    Modifier.fillMaxWidth().height(SheetMetrics.skeletonHeight.dp),
                                    RoundedCornerShape(SheetMetrics.skeletonRadius.dp),
                                )
                                CfSkeleton(
                                    Modifier.fillMaxWidth().height(SheetMetrics.skeletonHeight.dp),
                                    RoundedCornerShape(SheetMetrics.skeletonRadius.dp),
                                )
                            }
                        }
                    }

                    AirportSearchUi.Empty -> {
                        EmptyAirports()
                    }

                    is AirportSearchUi.Failed -> {
                        FailedAirports(retryEnabled, retry)
                    }

                    is AirportSearchUi.Results -> {}
                }
            }
        }
    }
}

@Composable
internal fun AirportResultRow(
    airport: AirportRowUi,
    choose: (ApiAirportSuggestion) -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(
                min = SheetMetrics.row.dp,
            ).testTag("wizard.airport.${airport.iata}")
            .cfClickable({
                choose(airport.airport)
            })
            .padding(horizontal = SheetMetrics.rowHorizontal.dp, vertical = SheetMetrics.rowVertical.dp),
        horizontalArrangement = Arrangement.spacedBy(SheetMetrics.rowGap.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            airport.iata,
            Modifier.widthIn(min = SheetMetrics.codeWidth.dp).testTag("wizard.airport.code.${airport.iata}"),
            style = CfTheme.type.style(CfTypeScale.wizardSearchCode),
            color = CfTheme.colors.foreground,
        )
        Column(Modifier.weight(1f)) {
            Text(
                airport.name,
                style = CfTheme.type.style(CfTypeScale.wizardSearchName),
                color = CfTheme.colors.foreground,
            )
            Text(
                airportPlace(airport, locale),
                style = CfTheme.type.style(CfTypeScale.wizardHint),
                color = CfTheme.colors.mutedForeground,
            )
        }
    }
}

@Composable
private fun EmptyAirports() {
    val border = CfTheme.colors.border
    Column(
        Modifier
            .fillMaxWidth()
            .testTag(
                "wizard.airport.empty",
            ).semantics { liveRegion = LiveRegionMode.Polite }
            .drawWithCache {
                val radius = SheetMetrics.stateRadius.dp.toPx()
                val stroke =
                    Stroke(
                        AuthTokens.focusWidth.dp.toPx(),
                        pathEffect =
                            PathEffect.dashPathEffect(
                                floatArrayOf(SheetMetrics.dash.dp.toPx(), SheetMetrics.dashGap.dp.toPx()),
                            ),
                    )
                onDrawBehind { drawRoundRect(border, cornerRadius = CornerRadius(radius), style = stroke) }
            }.padding(horizontal = SheetMetrics.emptyHorizontal.dp, vertical = SheetMetrics.emptyVertical.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SheetMetrics.statusGap.dp),
    ) {
        val large = CfTheme.type.style(CfTypeScale.screenBodyLg)
        Text(
            stringResource(R.string.android_wizard_search_empty_title),
            style = CfTheme.type.screenLabel.copy(fontSize = large.fontSize, lineHeight = large.lineHeight),
            color = CfTheme.colors.foreground,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.android_wizard_search_empty_body),
            style = CfTheme.type.screenBody,
            color = CfTheme.colors.mutedForeground,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FailedAirports(
    retryEnabled: Boolean,
    retry: () -> Unit,
) {
    val shape = RoundedCornerShape(SheetMetrics.stateRadius.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .testTag("wizard.airport.error")
            .semantics {
                liveRegion = LiveRegionMode.Polite
            }.background(
                CfTheme.colors.surface2,
                shape,
            ).border(
                AuthTokens.borderWidth.dp,
                CfTheme.colors.border,
                shape,
            ).padding(SheetMetrics.errorInset.dp),
        verticalArrangement = Arrangement.spacedBy(SheetMetrics.errorGap.dp),
    ) {
        val title = stringResource(R.string.android_wizard_search_error_title)
        val body = stringResource(R.string.android_wizard_search_error_body)
        val bold = SpanStyle(fontWeight = CfTheme.type.screenLabel.fontWeight)
        Row(horizontalArrangement = Arrangement.spacedBy(SheetMetrics.statusGap.dp)) {
            Icon(
                LucideGlyphs.CircleAlert.imageVector(),
                null,
                Modifier.offset(y = SheetMetrics.errorIconTop.dp).size(SheetMetrics.statusIcon.dp),
                tint = CfTheme.colors.destructive,
            )
            Text(
                buildAnnotatedString {
                    withStyle(bold) { append(title) }
                    append(" ")
                    append(body)
                },
                style = CfTheme.type.screenBody,
                color = CfTheme.colors.foreground,
            )
        }
        CfButton(
            WizardCopy("Try again").resolve(),
            retry,
            Modifier.testTag("wizard.airport.retry"),
            style = CfButtonStyle.Outline,
            size = CfButtonSize.Lg,
            fillWidth = true,
            enabled = retryEnabled,
        )
    }
}
