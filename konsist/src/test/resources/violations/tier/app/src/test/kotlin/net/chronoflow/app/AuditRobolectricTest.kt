package net.chronoflow.app

import org.junit.Test
import net.chronoflow.core.testing.tiers.Audit
import net.chronoflow.core.testing.tiers.Smoke
import net.chronoflow.core.testing.tiers.Layout
import org.robolectric.annotation.Config

@Layout
@Audit
@Config(sdk = [36])
class AuditRobolectricTest {
    @Test fun auditCase() = Unit
}
