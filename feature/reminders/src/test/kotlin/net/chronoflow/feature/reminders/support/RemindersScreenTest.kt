package net.chronoflow.feature.reminders.support

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.junit.measureClassWarmup
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.RuleChain
import org.robolectric.Robolectric
import org.robolectric.android.controller.ActivityController

abstract class RemindersScreenTest(
    protected val profile: DeviceProfile = DeviceProfiles.pixel8,
    language: AppLanguage = AppLanguage.En,
    protected val dark: Boolean = false,
) {
    protected val compose = createEmptyComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(profile, language, dark)).around(compose)
    protected lateinit var host: ActivityController<ComponentActivity>
    protected val activity get() = host.get()

    @Before fun createHost() {
        warmNativeRuntime()
        host =
            Robolectric.buildActivity(ComponentActivity::class.java).setup()
    }

    @After fun closeHost() {
        host.pause().stop().destroy()
    }

    protected fun show(content: @Composable () -> Unit) {
        activity.setContent {
            ProfileHost(profile) { ChronoFlowTheme(darkTheme = dark, reduceMotion = true, content = content) }
        }
        compose.waitForIdle()
    }

    companion object {
        private val nativeReady by lazy {
            measureClassWarmup(RemindersScreenTest::class.java, 36, "native-font-runtime") {
                org.robolectric.nativeruntime.DefaultNativeRuntimeLoader
                    .injectAndLoad()
                android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
            }
        }

        @JvmStatic fun warmNativeRuntime() {
            nativeReady
        }
    }
}
