package net.chronoflow.feature.today

import androidx.compose.runtime.Immutable
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.DayStatus
import net.chronoflow.core.plan.TripListValue
import net.chronoflow.core.ui.trip.TripCardUi
import java.time.Instant

@Immutable
sealed interface TodayUiState {
    @Immutable data object Loading : TodayUiState

    @Immutable data object Empty : TodayUiState

    @Immutable data class Completed(
        val route: String,
        val endDate: String,
    ) : TodayUiState

    @Immutable data class Future(
        val hero: TripListValue,
        val next: NextStepUi?,
        val preparationDays: Int?,
        val planStartUtc: String,
        val planStartZone: String,
    ) : TodayUiState

    @Immutable data class Active(
        val hero: TripCardUi,
        val panel: TodayPanelUi?,
    ) : TodayUiState
}

@Immutable
data class NextStepUi(
    val blockId: String,
    val type: BlockType,
    val title: String,
    val startUtc: Instant,
    val startText: String,
)

@Immutable
data class TodayPanelUi(
    val dayLabel: String?,
    val dayStatus: DayStatus?,
    val progress: ProgressUi?,
    val current: List<TodayRowUi>,
    val upcoming: List<TodayRowUi>,
    val hidden: HiddenNotice,
)

@Immutable
data class ProgressUi(
    val done: Int,
    val total: Int,
    val skipped: Int,
)

enum class HiddenNotice { None, Some, All }

@Immutable
data class TodayRowUi(
    val blockId: String,
    val type: BlockType,
    val actionable: Boolean,
    val title: String,
    val zone: String,
    val running: Boolean,
    val target: Instant,
    val timeText: String,
    val mark: CheckStatus?,
    val markedTimeText: String,
)

@Immutable
data class StepSelection(
    val tripId: String,
    val blockId: String,
)
