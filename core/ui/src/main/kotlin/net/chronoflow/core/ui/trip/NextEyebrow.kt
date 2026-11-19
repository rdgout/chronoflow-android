@file:Suppress("ktlint:compose:compositionlocal-allowlist")

package net.chronoflow.core.ui.trip

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.ui.R
import net.chronoflow.core.ui.clock.MinuteClock
import java.time.Duration
import java.time.Instant
import java.util.Locale
import kotlin.math.ceil

sealed interface NextLabel {
    data object Now : NextLabel

    data class In(
        val minutes: Int,
    ) : NextLabel
}

fun nextEyebrow(
    startUtc: Instant,
    now: Instant,
): NextLabel =
    if (startUtc <= now) {
        NextLabel.Now
    } else {
        NextLabel.In(ceil(Duration.between(now, startUtc).toMillis() / 60_000.0).toInt().coerceAtLeast(1))
    }

fun interface TripCardProbe {
    fun composed(id: String)
}

val LocalTripCardProbe = staticCompositionLocalOf<TripCardProbe?> { null }

@Composable
fun NextEyebrow(
    startUtc: Instant,
    clock: MinuteClock,
    formatter: CopyLineFormatter,
    language: Language,
    id: String,
    modifier: Modifier = Modifier,
) {
    val probe = LocalTripCardProbe.current
    SideEffect { probe?.composed("eyebrow.$id") }
    val label by remember(startUtc, clock) { derivedStateOf { nextEyebrow(startUtc, clock.now) } }
    val text =
        when (val value = label) {
            NextLabel.Now -> {
                stringResource(R.string.screens_next_now)
            }

            is NextLabel.In -> {
                stringResource(
                    R.string.screens_next_in_duration,
                    formatter.formatMinutes(value.minutes.toDouble(), language, duration = true),
                )
            }
        }
    Text(
        text.uppercase(if (language == Language.Nl) Locale.forLanguageTag("nl") else Locale.ENGLISH),
        modifier = modifier,
        color = CfTheme.colors.dawnText,
        style = CfTheme.type.style(CfTypeScale.screenEyebrow),
    )
}
