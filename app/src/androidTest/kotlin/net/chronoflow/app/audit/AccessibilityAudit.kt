package net.chronoflow.app.audit

import android.util.Log
import android.view.View
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.accessibility.disableAccessibilityChecks
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.core.app.ActivityScenario
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult.AccessibilityCheckResultType.ERROR
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultDescriptor
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityViewCheckResult
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import com.google.android.apps.common.testing.accessibility.framework.uielement.ViewHierarchyElement
import net.chronoflow.core.testing.audit.AuditCheck
import net.chronoflow.core.testing.audit.AuditFinding
import net.chronoflow.core.testing.audit.AuditPolicy
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher

@OptIn(ExperimentalTestApi::class)
fun ComposeTestRule.runAccessibilityAudit(
    scenario: ActivityScenario<*>,
    contentTag: String = "app.root",
) {
    scenario.onActivity { check(!it.isFinishing) { "audit host is finishing" } }
    awaitHardwareFrames()
    val logged = mutableSetOf<String>()
    val validator =
        AccessibilityValidator()
            .setRunChecksFromRootView(true)
            .setThrowExceptionFor(ERROR)
            .setSaveImages(false)
            .setResultDescriptor(SafeAuditDescriptor())
            .setSuppressingResultMatcher(
                object : TypeSafeMatcher<AccessibilityViewCheckResult>() {
                    override fun describeTo(description: Description) {
                        description.appendText("the five documented ChronoFlow accessibility rules")
                    }

                    override fun matchesSafely(result: AccessibilityViewCheckResult): Boolean {
                        val finding = result.finding()
                        val rule = AuditPolicy.shouldIgnore(finding) ?: return false
                        val line =
                            "check=${finding.checkName} tag=${finding.testTag ?: "none"} " +
                                "rule=$rule bounds=${result.safeBounds()}"
                        if (logged.add(line)) Log.i("CfAudit", line)
                        return true
                    }
                },
            )
    enableAccessibilityChecks(validator)
    try {
        onNodeWithTag(contentTag).tryPerformAccessibilityChecks()
    } finally {
        disableAccessibilityChecks()
    }
}

private fun AccessibilityViewCheckResult.finding(): AuditFinding {
    val name = accessibilityHierarchyCheck?.simpleName ?: sourceCheckClass.simpleName
    val node = element
    val bounds = node?.boundsInScreen
    val window = node?.window?.boundsInScreen
    return AuditFinding(
        check =
            when (name) {
                "TextContrastCheck" -> AuditCheck.TextContrast
                "ImageContrastCheck" -> AuditCheck.ImageContrast
                "TouchTargetSizeCheck" -> AuditCheck.TouchTarget
                "TextSizeCheck" -> AuditCheck.TextSize
                else -> AuditCheck.Other
            },
        checkName = name,
        testTag =
            node?.resourceName?.substringAfterLast('/')?.takeIf(String::isNotBlank)
                ?: node?.testTag?.toString()?.takeIf(String::isNotBlank),
        enabled = node?.isEnabled ?: view?.isEnabled ?: true,
        actionable =
            node?.let { it.isClickable || it.isCheckable == true }
                ?: view?.let { it.isClickable || it is android.widget.Checkable }
                ?: false,
        fullyVisible = if (bounds != null && window != null) !bounds.isEmpty && window.contains(bounds) else true,
    )
}

private fun AccessibilityViewCheckResult.safeBounds(): String =
    element?.boundsInScreen?.let { "${it.left},${it.top},${it.right},${it.bottom}" } ?: "unknown"

// Framework descriptions can include editable text; audit output contains structural identifiers only.
private class SafeAuditDescriptor : AccessibilityCheckResultDescriptor() {
    override fun describeResult(result: AccessibilityCheckResult): String =
        if (result is AccessibilityViewCheckResult) {
            val finding = result.finding()
            "check=${finding.checkName} tag=${finding.testTag ?: "none"} bounds=${result.safeBounds()}"
        } else {
            "check=${result.sourceCheckClass.simpleName}"
        }

    override fun describeView(view: View?): String = "viewId=${view?.id ?: "none"}"

    override fun describeElement(element: ViewHierarchyElement?): String = "resource=${element?.resourceName ?: "none"}"
}
