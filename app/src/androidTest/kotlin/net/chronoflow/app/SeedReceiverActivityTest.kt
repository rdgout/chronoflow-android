package net.chronoflow.app

import android.content.Intent
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import net.chronoflow.core.testing.tiers.Smoke
import net.chronoflow.debug.SeedReceiverActivity
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalTestApi::class)
@Smoke
class SeedReceiverActivityTest {
    @get:Rule val compose = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    private fun activities(stage: Stage) = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(stage)

    private fun receiverAlive(): Boolean {
        var alive = true
        instrumentation.runOnMainSync {
            alive =
                listOf(Stage.CREATED, Stage.STARTED, Stage.RESUMED, Stage.PAUSED, Stage.STOPPED)
                    .any { stage -> activities(stage).any { it is SeedReceiverActivity } }
        }
        return alive
    }

    @Test
    fun receiverSeedsThenHandsOverToMainActivity() {
        val context = instrumentation.targetContext
        val intent =
            Intent(context, SeedReceiverActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra("CF_AUTH_TEST_ID", UUID.randomUUID().toString())
                .putExtra("CF_UI_TEST_PROFILE", "screen")
                .putExtra("cf.settings.language", "system")
        context.startActivity(intent)
        compose.waitUntilExactlyOneExists(hasTestTag("app.root"), 5_000)
        var mainResumed = false
        instrumentation.runOnMainSync { mainResumed = activities(Stage.RESUMED).any { it is MainActivity } }
        // finish() is asynchronous: the receiver is destroyed shortly after MainActivity appears.
        compose.waitUntil(5_000) { !receiverAlive() }
        assertTrue(mainResumed)
    }

    @After
    fun finishMainActivity() {
        instrumentation.runOnMainSync { activities(Stage.RESUMED).forEach { it.finish() } }
    }
}
