@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.app.settings

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.app.ConfigurationPolicy
import net.chronoflow.core.datastore.DevicePreferences
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.config.LanguageChoice
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.junit.measureClassWarmup
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.feature.settings.language.LanguageRoute
import net.chronoflow.feature.settings.language.LanguageSettingsViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.time.Duration.Companion.seconds

@Layout
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], application = android.app.Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LanguageRouteRecreationTest {
    private val compose = createEmptyComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)

    @Test fun choosingDutchRecreatesThroughTheRouteAndAcknowledgesTheRequestOnce() =
        runTest(timeout = 60.seconds) {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val context: Context = RuntimeEnvironment.getApplication()
            val preferences = DevicePreferences(context, RouteLanguageStore(), dispatcher)
            preferences.setLanguage(LanguageChoice.System, 29)
            val fixture =
                LanguageRouteFixture(
                    AppLanguageApplier(context, preferences, dispatcher, LanguageFeedback()),
                    dispatcher,
                )
            LanguageRouteActivity.fixture = fixture
            val host = Robolectric.buildActivity(LanguageRouteActivity::class.java).setup()
            try {
                compose.runOnIdle { runCurrent() }
                val first = host.get()
                val model = checkNotNull(fixture.model)
                assertEquals(LanguageChoice.System, model.ui.value.choice)
                compose.onNodeWithTag("settings.language.choice.nl").performClick()
                compose.runOnIdle { runCurrent() }
                compose.waitForIdle()
                compose.runOnIdle { runCurrent() }
                assertEquals(1, fixture.recreates)
                assertNotSame(first, fixture.activity)
                assertSame(model, fixture.model)
                assertEquals(
                    "nl",
                    fixture.activity
                        ?.resources
                        ?.configuration
                        ?.locales
                        ?.get(0)
                        ?.language,
                )
                assertNull(model.ui.value.recreate)
                compose.waitForIdle()
                compose.runOnIdle { runCurrent() }
                assertEquals(1, fixture.recreates)
            } finally {
                withContext(NonCancellable) {
                    host.pause().stop().destroy()
                    fixture.model
                        ?.viewModelScope
                        ?.coroutineContext
                        ?.job
                        ?.cancelAndJoin()
                    LanguageRouteActivity.fixture = null
                }
            }
        }

    companion object {
        @BeforeClass
        @JvmStatic fun warmNativeRuntime() {
            measureClassWarmup(LanguageRouteRecreationTest::class.java, 29, "native-font-runtime") {
                org.robolectric.nativeruntime.DefaultNativeRuntimeLoader
                    .injectAndLoad()
                android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
            }
        }
    }
}

internal class LanguageRouteFixture(
    dependencies: AppLanguageApplier,
    dispatcher: CoroutineDispatcher,
) {
    var model: LanguageSettingsViewModel? = null
    var activity: LanguageRouteActivity? = null
    var recreates = 0
    val factory = viewModelFactory { initializer { LanguageSettingsViewModel(dependencies, dispatcher) } }
}

internal class LanguageRouteActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase)
        applyOverrideConfiguration(ConfigurationPolicy.overrideConfiguration(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val current = checkNotNull(fixture)
        current.activity = this
        setContent {
            ProfileHost(DeviceProfiles.pixel8) {
                ChronoFlowTheme(reduceMotion = true) {
                    val model: LanguageSettingsViewModel = viewModel(factory = current.factory)
                    SideEffect { current.model = model }
                    LanguageRoute(model, {})
                }
            }
        }
    }

    override fun recreate() {
        checkNotNull(fixture).recreates++
        super.recreate()
    }

    companion object {
        var fixture: LanguageRouteFixture? = null
    }
}

private class RouteLanguageStore : DataStore<Preferences> {
    override val data = MutableStateFlow(emptyPreferences())

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
        transform(data.value).also { data.value = it }
}
