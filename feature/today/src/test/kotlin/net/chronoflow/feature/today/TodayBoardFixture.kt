package net.chronoflow.feature.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.chronoflow.core.datamock.MockAppSeed
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.icon.MaterialSymbols
import net.chronoflow.core.designsystem.layout.LocalSafeInsets
import net.chronoflow.core.designsystem.system.CfNavItem
import net.chronoflow.core.designsystem.system.CfNavLayout
import net.chronoflow.core.designsystem.system.CfNavigationSuiteScaffold
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.CheckMark
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.Priority
import net.chronoflow.core.model.contracts.TripStatus
import net.chronoflow.core.plan.CopyFormat
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.plan.TripListValue
import net.chronoflow.core.plan.blockInterval
import net.chronoflow.core.plan.planDate
import net.chronoflow.core.plan.trippage.PlanPreferences
import net.chronoflow.core.plan.zoneLabel
import net.chronoflow.core.ui.R
import net.chronoflow.core.ui.clock.MinuteClock
import net.chronoflow.feature.today.ui.TodayStepRow

internal object TodayBoardFixture {
    private val account by lazy {
        MockAppSeed
            .make(todayNow, walkthroughSeen = true)
            .accounts.values
            .minBy { it.snapshot.id }
    }
    val detail by lazy {
        val id =
            account.trips
                .firstOrNull { it.card.progress.status == TripStatus.Active }
                ?.card
                ?.id
                ?: account.trips
                    .first()
                    .card.id
        account.details.getValue(id)
    }

    fun state(
        name: String,
        formatter: CopyLineFormatter,
        language: Language,
    ): TodayUiState {
        var trips =
            account.trips.map {
                TripListValue(
                    it.card,
                    it.planStartUtc,
                    it.planEndUtc,
                    it.card.progress.status,
                    it.planStartZone,
                    it.planEndDate,
                    it.preparationDays,
                )
            }
        var detail = detail
        var hidden = emptyList<BlockType>()
        when (name) {
            "empty" -> {
                trips = emptyList()
            }

            "completed" -> {
                trips = trips.filter { it.status == TripStatus.Completed }
            }

            "future" -> {
                trips = trips.filter { it.status == TripStatus.Upcoming }
            }

            "done", "skipped" -> {
                val block =
                    detail.plan.blocks.first {
                        it.actionable && it.checkKey != null &&
                            planDate(it.endUtc)!! > todayNow
                    }
                val status = if (name == "done") CheckStatus.Done else CheckStatus.Skipped
                detail =
                    detail.copy(
                        checkState =
                            detail.checkState.copy(
                                marks = listOf(CheckMark(block.checkKey!!, status, todayNow.toString())),
                            ),
                    )
            }

            "hidden" -> {
                hidden =
                    detail.plan.blocks
                        .filter { it.priority == Priority.Core }
                        .map { it.type }
            }

            "allHidden" -> {
                hidden = BlockType.known
            }

            "nothingNow" -> {
                detail =
                    detail.copy(
                        plan =
                            detail.plan.copy(
                                blocks =
                                    detail.plan.blocks.filter {
                                        planDate(it.startUtc)!! >
                                            todayNow
                                    },
                            ),
                    )
            }
        }
        return TodayReducer.reduce(
            TodayInput(
                trips,
                name != "loading",
                PlanPreferences(hiddenCategories = hidden),
                detail,
                todayNow,
                language,
                formatter,
            ),
        )
    }

    fun row(
        formatter: CopyLineFormatter,
        language: Language,
    ): TodayRowUi {
        val block = detail.plan.blocks.first { it.actionable && it.checkKey != null }
        val options = CopyFormat(language, displayZone = block.displayTz, trip = detail.trip)
        val instantOnly =
            block.type == BlockType.CaffeineServing || block.type == BlockType.ZoneChange ||
                block.startUtc == block.endUtc
        return TodayRowUi(
            block.id,
            block.type,
            true,
            formatter.format(block.text, options),
            zoneLabel(block.displayTz, detail.trip),
            true,
            planDate(block.endUtc)!!,
            formatter.formatInstant(block.endUtc, options),
            null,
            if (instantOnly) {
                formatter.formatInstant(
                    block.startUtc,
                    options,
                )
            } else {
                formatter.blockInterval(block, options, separator = " → ")
            },
        )
    }

    val rowNow get() =
        planDate(
            detail.plan.blocks
                .first { it.actionable && it.checkKey != null }
                .startUtc,
        )!!
}

@Composable
internal fun TodayRows(
    row: TodayRowUi,
    clock: MinuteClock,
    language: Language,
    formatter: CopyLineFormatter,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(CfTheme.colors.card)
            .windowInsetsPadding(LocalSafeInsets.current)
            .verticalScroll(rememberScrollState())
            .padding(PlanUITokens.space4.dp)
            .testTag("today.rows"),
        verticalArrangement = Arrangement.spacedBy(PlanUITokens.space3.dp),
    ) {
        TodayStepRow(row.copy(blockId = "running"), clock, language, formatter, {}, Modifier.fillMaxWidth())
        TodayStepRow(row.copy(blockId = "done", mark = CheckStatus.Done), clock, language, formatter, {
        }, Modifier.fillMaxWidth())
        TodayStepRow(row.copy(blockId = "skipped", mark = CheckStatus.Skipped), clock, language, formatter, {
        }, Modifier.fillMaxWidth())
    }
}

@Composable
internal fun TodayShellFixture(content: @Composable () -> Unit) {
    val safe = LocalSafeInsets.current
    val contentInsets = safe.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    val items =
        listOf(
            CfNavItem(
                stringResource(R.string.screens_today),
                MaterialSymbols.SpaceDashboard,
                MaterialSymbols.SpaceDashboardFill,
                "tab.today",
            ),
            CfNavItem(
                stringResource(R.string.screens_my_trips),
                MaterialSymbols.Flight,
                MaterialSymbols.FlightFill,
                "tab.trips",
            ),
            CfNavItem(
                stringResource(R.string.screens_settings),
                MaterialSymbols.Settings,
                MaterialSymbols.SettingsFill,
                "tab.settings",
            ),
        )
    CfNavigationSuiteScaffold(
        CfNavLayout.Bar,
        items,
        0,
        {},
        Modifier.windowInsetsPadding(safe.only(WindowInsetsSides.Bottom)),
    ) {
        CompositionLocalProvider(LocalSafeInsets provides contentInsets, content = content)
    }
}
