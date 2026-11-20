@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.debug

import kotlinx.coroutines.yield
import net.chronoflow.app.wizard.WizardStartOverride
import net.chronoflow.core.wizard.model.AirportSearchService
import net.chronoflow.feature.wizard.TripWizardDependencies
import net.chronoflow.feature.wizard.debug.FixedAirportSearch
import net.chronoflow.feature.wizard.debug.WizardBoardCaptureSeed
import net.chronoflow.feature.wizard.debug.WizardMemoryDraftStore
import java.time.Instant
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.roundToLong

internal class WizardDebugSeeds {
    data class Prepared(
        val override: WizardStartOverride?,
        val open: Boolean,
    )

    private var last: Pair<Long, String?>? = null
    private var installed: WizardStartOverride? = null

    fun prepare(
        seed: TestSeed?,
        epoch: Long,
        locale: Locale,
    ): Prepared {
        val key = epoch to seed?.testId
        if (last == key) return Prepared(installed, false)
        last = key
        installed = null
        val board = seed?.wizardBoardCapture
        if (board == null) return Prepared(null, seed?.wizardAutoOpen == true)
        if (!board.startsWith("Wizard")) return Prepared(null, false)
        if (!WizardBoardCaptureSeed.knows(board)) {
            java.util.logging.Logger
                .getLogger("Wizard")
                .warning("Unknown wizard board capture")
            return Prepared(null, true)
        }
        val override =
            object : WizardStartOverride {
                override val presentation = WizardBoardCaptureSeed.presentation(board)

                override fun dependencies(session: TripWizardDependencies): TripWizardDependencies {
                    val memory = WizardMemoryDraftStore()
                    val search = FixedAirportSearch(board)
                    val searchClock = AtomicReference(WizardBoardCaptureSeed.now)
                    return object : TripWizardDependencies by session {
                        override val store = memory
                        override val now: () -> Instant = { WizardBoardCaptureSeed.now }
                        override val airportSearch =
                            AirportSearchService(
                                search::results,
                                { searchClock.get() },
                                sleep = { seconds ->
                                    searchClock.updateAndGet { it.plusMillis((seconds * 1000).roundToLong()) }
                                    yield()
                                },
                            )

                        override suspend fun start() = WizardBoardCaptureSeed.draft(board, locale)
                    }
                }
            }
        installed = override
        return Prepared(override, true)
    }
}
