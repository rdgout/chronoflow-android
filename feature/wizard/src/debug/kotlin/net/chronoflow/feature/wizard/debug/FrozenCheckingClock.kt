package net.chronoflow.feature.wizard.debug

import kotlinx.coroutines.awaitCancellation
import net.chronoflow.core.wizard.model.WizardCheckingClock

object FrozenCheckingClock {
    val clock = WizardCheckingClock(nowMillis = { 0 }, sleepUntilMillis = { awaitCancellation() })
}
