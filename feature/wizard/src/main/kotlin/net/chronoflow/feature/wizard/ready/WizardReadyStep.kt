package net.chronoflow.feature.wizard.ready

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.delay
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.feature.wizard.components.WizardBanner
import net.chronoflow.feature.wizard.components.WizardBannerTone
import net.chronoflow.feature.wizard.components.rememberServerCopy
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.ui.WizardCopy
import net.chronoflow.feature.wizard.ui.resolve

@Composable
internal fun WizardReadyStep(
    ui: ReadyUi,
    formatter: CopyLineFormatter?,
    onEvent: (WizardEvent) -> Unit,
) {
    val events by rememberUpdatedState(onEvent)
    LaunchedEffect(Unit) {
        while (true) {
            events(WizardEvent.ReadyTick)
            delay(ReadyMetrics.tickMillis)
        }
    }
    val started = remember { BooleanArray(1) }
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        if (started[0]) events(WizardEvent.ReadyForeground) else started[0] = true
    }
    Column(
        Modifier.testTag("wizard.ready"),
        verticalArrangement = Arrangement.spacedBy(PlanUITokens.wizardContentGap.dp),
    ) {
        ui.banner?.let { ReadyBanner(it, formatter) }
        ui.summary?.let { ReadySummaryCard(it, formatter) }
        ReadyRowsCard(ui.rowsUi, onEvent)
        ui.price?.let { ReadyPriceBlock(it) }
    }
}

@Composable
private fun ReadyBanner(
    ui: ReadyBannerUi,
    formatter: CopyLineFormatter?,
) {
    val serverCopy = rememberServerCopy(formatter)
    when (ui) {
        is ReadyBannerUi.SaveFailed -> {
            WizardBanner(ui.body.resolve(), ui.tag, title = ui.title.resolve())
        }

        is ReadyBannerUi.Adjusted -> {
            WizardBanner(
                ui.reasons.joinToString(" ") { serverCopy(it) },
                "wizard.ready.notice",
                tone = WizardBannerTone.Info,
                title = WizardCopy("We adjusted your plan").resolve(),
            )
        }

        is ReadyBannerUi.Updated -> {
            val summary = ui.summary?.let(serverCopy).orEmpty()
            val price = ui.price?.let { WizardCopy("Price").resolve() + ": " + it.copy.resolve() }.orEmpty()
            WizardBanner(
                listOf(summary, price).filter { it.isNotEmpty() }.joinToString(" "),
                "wizard.ready.notice",
                tone = WizardBannerTone.Info,
                title = WizardCopy("Updated").resolve(),
            )
        }
    }
}
