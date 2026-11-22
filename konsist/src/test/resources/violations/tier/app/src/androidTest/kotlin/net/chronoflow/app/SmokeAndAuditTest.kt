package net.chronoflow.app

import org.junit.Test
import net.chronoflow.core.testing.tiers.Audit
import net.chronoflow.core.testing.tiers.Smoke
import net.chronoflow.core.testing.tiers.Layout
import org.robolectric.annotation.Config

@Smoke
@Audit
class SmokeAndAuditTest {
    @Test fun auditCase() = Unit
}
