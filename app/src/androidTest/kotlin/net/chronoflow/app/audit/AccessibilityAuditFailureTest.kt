package net.chronoflow.app.audit

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityViewCheckException
import net.chronoflow.app.testing.ScreenTest
import net.chronoflow.core.testing.tiers.Audit
import net.chronoflow.core.ui.R
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

@Audit
@OptIn(ExperimentalTestApi::class)
class AccessibilityAuditFailureTest : ScreenTest() {
    @Test fun anEnabledUndersizedControlFailsTheRealFrameworkAudit() {
        val scenario = launch(mapOf("CF_AUTH_TEST_AUDIT_SCREEN" to "sign-in"))
        compose.waitUntilExactlyOneExists(hasTestTag("sign-in.email"), 5_000)
        lateinit var control: Button
        lateinit var parent: ViewGroup
        scenario.onActivity { activity ->
            parent = activity.window.decorView as ViewGroup
            control =
                Button(activity).apply {
                    id = View.generateViewId()
                    contentDescription = activity.getString(R.string.auth_sign_in)
                    setOnClickListener {}
                }
            parent.addView(control, FrameLayout.LayoutParams(8, 8, Gravity.CENTER))
        }
        try {
            val failure =
                assertThrows(AccessibilityViewCheckException::class.java) {
                    compose.runAccessibilityAudit(scenario, "sign-in.email")
                }
            assertTrue(failure.results.any { it.accessibilityHierarchyCheck?.simpleName == "TouchTargetSizeCheck" })
        } finally {
            scenario.onActivity { parent.removeView(control) }
        }
    }
}
