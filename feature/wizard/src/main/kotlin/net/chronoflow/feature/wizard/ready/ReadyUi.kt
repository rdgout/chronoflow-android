@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.feature.wizard.ready

import androidx.compose.runtime.Immutable
import net.chronoflow.core.model.auth.LockoutWait
import net.chronoflow.core.model.contracts.CopyLine
import net.chronoflow.core.wizard.model.WizardCredits
import net.chronoflow.core.wizard.model.WizardSaveFailure
import net.chronoflow.feature.wizard.ui.WizardCopy

@Immutable
data class ReadyText(
    val pattern: String,
    val copies: List<WizardCopy> = emptyList(),
) {
    companion object {
        internal fun capture(block: ((String, Map<String, String>) -> String) -> String): ReadyText {
            val copies = mutableListOf<WizardCopy>()
            val pattern =
                block { key, args ->
                    val copy = WizardCopy(key, args)
                    val index = copies.indexOf(copy).takeIf { it >= 0 } ?: copies.size.also { copies.add(copy) }
                    marker(index)
                }
            return ReadyText(pattern, copies.toList())
        }

        internal fun marker(index: Int) = "\u0000$index\u0000"
    }
}

enum class ReadyRow(
    val label: String,
    val id: String,
) {
    Route("Route", "route"),
    Flight("Flight", "flight"),
    Return("Return", "return"),
    Sleep("Sleep", "sleep"),
    Extras("Extras", "extras"),
    LatestWake("Latest wake-up", "latest-wake-up"),
}

@Immutable
data class ReadyRowUi(
    val kind: ReadyRow,
    val value: ReadyText,
)

@Immutable
data class ReadyRowsUi(
    val rows: List<ReadyRowUi>,
    val enabled: Boolean,
)

@Immutable
data class ReadySummaryUi(
    val line: CopyLine,
    val dates: ReadyText,
)

@Immutable
sealed interface PriceValue {
    data object Unlimited : PriceValue

    data object Free : PriceValue

    data object OneCredit : PriceValue

    data class Credits(
        val n: Int,
    ) : PriceValue

    val copy: WizardCopy get() =
        when (this) {
            Unlimited -> WizardCopy("Included in your subscription.")
            Free -> WizardCopy("Free")
            OneCredit -> WizardCopy("{{count}} credit", mapOf("count" to "1"))
            is Credits -> WizardCopy("{{count}} credits", mapOf("count" to n.toString()))
        }
}

@Immutable
sealed interface PriceLine {
    data object FreeTrip : PriceLine

    data object FreeHome : PriceLine

    data class Balance(
        val n: Int,
        val alert: Boolean,
    ) : PriceLine

    val copy: WizardCopy get() =
        when (this) {
            FreeTrip -> WizardCopy("This trip is free: only a small body-clock shift is needed.")
            FreeHome -> WizardCopy("Your flight home is free: only a small shift is needed that way.")
            is Balance -> WizardCopy("You have {{count}} credits", mapOf("count" to n.toString()))
        }
}

@Immutable
data class ReadyPriceUi(
    val showLabel: Boolean,
    val value: PriceValue,
    val lines: List<PriceLine>,
)

@Immutable
sealed interface ReadyBannerUi {
    data class SaveFailed(
        val kind: WizardSaveFailure.Kind,
        val retrySeconds: Int,
    ) : ReadyBannerUi {
        val tag: String get() = "wizard.save.${kind.name}"
        val title: WizardCopy get() =
            WizardCopy(
                when (kind) {
                    WizardSaveFailure.Kind.offline -> "Saving needs a connection"
                    WizardSaveFailure.Kind.server -> "We couldn't save your plan"
                    WizardSaveFailure.Kind.throttle -> "Wait a moment"
                },
            )
        val body: WizardCopy get() =
            when (kind) {
                WizardSaveFailure.Kind.offline -> {
                    WizardCopy(
                        "Connect to the internet and tap Make my plan again. Keep this screen open: closing discards the trip.",
                    )
                }

                WizardSaveFailure.Kind.server -> {
                    WizardCopy("Something went wrong on our side. Tap Make my plan to try again.")
                }

                WizardSaveFailure.Kind.throttle -> {
                    if (retrySeconds > 0) {
                        val wait = LockoutWait(retrySeconds)
                        WizardCopy(
                            wait.localizationKey,
                            mapOf(
                                "count" to wait.count.toString(),
                                "%1" to wait.count.toString(),
                            ),
                        )
                    } else {
                        WizardCopy("Too many attempts. Please try again later.")
                    }
                }
            }
    }

    data class Adjusted(
        val reasons: List<CopyLine>,
    ) : ReadyBannerUi {
        fun body(resolve: (CopyLine) -> String): String = reasons.joinToString(" ", transform = resolve)
    }

    data class Updated(
        val summary: CopyLine?,
        val price: PriceValue?,
    ) : ReadyBannerUi {
        fun body(
            summary: String,
            price: String,
        ): String = listOf(summary, price).filter { it.isNotEmpty() }.joinToString(" ")
    }
}

@Immutable
sealed interface ReadyButtonLabel {
    data object MakePlan : ReadyButtonLabel

    data object Checking : ReadyButtonLabel

    data object Retry : ReadyButtonLabel

    data class Countdown(
        val seconds: Int,
    ) : ReadyButtonLabel

    val copy: WizardCopy get() =
        when (this) {
            MakePlan -> WizardCopy("Make my plan")
            Checking -> WizardCopy("Checking…")
            Retry -> WizardCopy("Try again")
            is Countdown -> WizardCopy("Try again in {{count}} s", mapOf("count" to seconds.toString()))
        }
}

@Immutable
sealed interface ReadyFooterUi {
    data object CloseOnly : ReadyFooterUi

    data class Primary(
        val label: ReadyButtonLabel,
        val enabled: Boolean,
        val busy: Boolean,
    ) : ReadyFooterUi
}

@Immutable
data class ReadyUi(
    val summary: ReadySummaryUi?,
    val rows: List<ReadyRowUi>,
    val price: ReadyPriceUi?,
    val banner: ReadyBannerUi?,
    val footer: ReadyFooterUi,
    val credits: WizardCredits?,
    val reviewing: Boolean,
) {
    val title: WizardCopy get() = WizardCopy("Ready to plan")
    val rowsUi: ReadyRowsUi get() = ReadyRowsUi(rows, !reviewing)
}
