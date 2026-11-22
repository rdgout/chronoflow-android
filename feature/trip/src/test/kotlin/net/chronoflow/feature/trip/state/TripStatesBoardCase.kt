package net.chronoflow.feature.trip.state

import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.BoardCaptureCell

class TripStatesBoardCase(
    val board: String,
    val profile: DeviceProfile,
    val language: AppLanguage,
    val dark: Boolean = false,
) {
    val theme get() = if (dark) "dark" else "light"

    override fun toString() = "$board-${profile.name}-${language.tag}-$theme"

    fun fixture() = TripStatesFixture(board, profile, language, dark)

    companion object {
        val boards =
            listOf(
                "PlanList",
                "PlanListScrolled",
                "PlanLoading",
                "PlanNoPlan",
                "PlanRemote",
                "PlanRemoteLoadingNL",
                "PlanRemoteOffline",
                "PlanRemoteOfflineNL",
                "ErrorNotFound",
                "ErrorServer",
            )

        fun guardCases(): List<TripStatesBoardCase> =
            boards.flatMap { board ->
                listOf(
                    TripStatesBoardCase(board, DeviceProfiles.small, AppLanguage.Nl),
                    TripStatesBoardCase(board, DeviceProfiles.pixel8, AppLanguage.En),
                    TripStatesBoardCase(board, DeviceProfiles.largeFont, AppLanguage.Nl),
                    TripStatesBoardCase(
                        board,
                        DeviceProfiles.small.copy(name = "minimum280", widthDp = 280),
                        AppLanguage.Nl,
                    ),
                )
            }

        fun captureCases(): List<TripStatesBoardCase> =
            boards.flatMap { board ->
                val cells =
                    if (board in setOf("PlanRemote", "PlanRemoteOffline", "PlanRemoteOfflineNL")) {
                        BoardCaptureCell.phoneWithWideSibling
                    } else {
                        BoardCaptureCell.standard
                    }
                cells.map { cell -> TripStatesBoardCase(board, cell.profile, cell.language, cell.dark) }
            }
    }
}
