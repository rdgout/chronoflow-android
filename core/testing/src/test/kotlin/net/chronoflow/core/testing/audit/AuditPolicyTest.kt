package net.chronoflow.core.testing.audit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuditPolicyTest {
    private fun finding(
        check: AuditCheck = AuditCheck.TextContrast,
        tag: String? = "other",
        enabled: Boolean = true,
        actionable: Boolean = false,
        fullyVisible: Boolean = true,
    ) = AuditFinding(check, check.name, tag, enabled, actionable, fullyVisible)

    @Test fun welcomeBoardContrastIsRuleOne() {
        assertEquals(1, AuditPolicy.shouldIgnore(finding(tag = "welcome.eyebrow")))
        assertEquals(1, AuditPolicy.shouldIgnore(finding(tag = "welcome.headline")))
        assertEquals(1, AuditPolicy.shouldIgnore(finding(tag = "welcome.copy")))
        assertEquals(1, AuditPolicy.shouldIgnore(finding(tag = "welcome.plan")))
        assertEquals(1, AuditPolicy.shouldIgnore(finding(tag = "welcome.signIn")))
    }

    @Test fun welcomeContrastOnOtherTagFails() {
        assertNull(AuditPolicy.shouldIgnore(finding(tag = "welcome.other")))
        assertNull(AuditPolicy.shouldIgnore(finding(AuditCheck.ImageContrast, "welcome.plan")))
    }

    @Test fun signInGuestOnlyContrast() {
        assertEquals(2, AuditPolicy.shouldIgnore(finding(tag = "sign-in.guest")))
        assertNull(AuditPolicy.shouldIgnore(finding(AuditCheck.TouchTarget, "sign-in.guest")))
        assertNull(AuditPolicy.shouldIgnore(finding(AuditCheck.ImageContrast, "sign-in.guest")))
    }

    @Test fun disabledOnlyForContrastAndTouchTarget() {
        assertEquals(3, AuditPolicy.shouldIgnore(finding(enabled = false, actionable = true)))
        assertEquals(3, AuditPolicy.shouldIgnore(finding(AuditCheck.ImageContrast, enabled = false, actionable = true)))
        assertEquals(3, AuditPolicy.shouldIgnore(finding(AuditCheck.TouchTarget, enabled = false, actionable = true)))
        assertNull(AuditPolicy.shouldIgnore(finding(AuditCheck.TouchTarget, enabled = true, actionable = true)))
        assertNull(AuditPolicy.shouldIgnore(finding(enabled = false, actionable = false)))
        assertNull(AuditPolicy.shouldIgnore(finding(AuditCheck.TouchTarget, enabled = false, actionable = false)))
        assertNull(AuditPolicy.shouldIgnore(finding(AuditCheck.Other, enabled = false, actionable = true)))
    }

    @Test fun partialContrastIsRuleFour() {
        assertEquals(4, AuditPolicy.shouldIgnore(finding(fullyVisible = false)))
        assertEquals(4, AuditPolicy.shouldIgnore(finding(AuditCheck.ImageContrast, fullyVisible = false)))
        assertNull(AuditPolicy.shouldIgnore(finding(AuditCheck.TouchTarget, fullyVisible = false)))
    }

    @Test fun textSizeAlwaysIgnored() {
        assertEquals(5, AuditPolicy.shouldIgnore(finding(AuditCheck.TextSize)))
    }

    @Test fun nativeSheetDismissStripOnlyIgnoresTouchTarget() {
        val native = finding(AuditCheck.TouchTarget, tag = null, actionable = true).copy(nativeSheetDismissStrip = true)
        assertEquals(6, AuditPolicy.shouldIgnore(native))
        assertNull(AuditPolicy.shouldIgnore(native.copy(check = AuditCheck.TextContrast)))
        assertNull(AuditPolicy.shouldIgnore(native.copy(check = AuditCheck.ImageContrast)))
        assertNull(AuditPolicy.shouldIgnore(native.copy(check = AuditCheck.Other)))
        assertNull(AuditPolicy.shouldIgnore(native.copy(testTag = "app.button")))
        assertNull(AuditPolicy.shouldIgnore(native.copy(actionable = false)))
    }

    @Test fun unknownCheckNeverIgnored() {
        assertNull(
            AuditPolicy.shouldIgnore(
                finding(AuditCheck.Other, "welcome.plan", enabled = false, actionable = true, fullyVisible = false),
            ),
        )
    }
}
