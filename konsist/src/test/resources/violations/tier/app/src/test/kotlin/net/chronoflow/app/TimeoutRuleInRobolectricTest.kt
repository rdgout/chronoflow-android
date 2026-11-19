package net.chronoflow.app

import net.chronoflow.core.testing.tiers.Logic
import org.junit.Rule
import org.junit.Test
import org.junit.rules.Timeout

@Logic
class TimeoutRuleInRobolectricTest {
    @get:Rule val timeout: Timeout = Timeout.seconds(60)

    @Test fun runs() = Unit
}
