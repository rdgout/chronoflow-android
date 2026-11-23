@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.settings.profile

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.settings.support.FakeSettingsDependencies
import net.chronoflow.feature.settings.support.SettingsScreenTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProfileNameBindingTest : SettingsScreenTest() {
    @Test fun rapidSuccessiveEditsAreNeverOverwrittenByAnOlderComposition() =
        runTest(timeout = 60.seconds) {
            withProfile(FakeSettingsDependencies()) { model ->
                lateinit var field: TextFieldState
                var typedAhead = false
                show {
                    val ui by model.ui.collectAsStateWithLifecycle()
                    val current = rememberProfileNameField(model, ui.editor)
                    SideEffect {
                        field = current
                        if (ui.editor?.name == "A" && !typedAhead) {
                            typedAhead = true
                            current.setTextAndPlaceCursorAtEnd("AB")
                            current.setTextAndPlaceCursorAtEnd("ABC")
                        }
                    }
                }
                compose.runOnIdle { field.setTextAndPlaceCursorAtEnd("A") }
                compose.runOnIdle { runCurrent() }
                compose.waitForIdle()
                compose.runOnIdle { runCurrent() }
                assertTrue(typedAhead)
                assertEquals(
                    "ABC",
                    model.ui.value.editor
                        ?.name,
                )
            }
        }

    @Test fun aLatePullReplacementCannotOverwriteAnEditMadeBeforeItsEffect() =
        runTest(timeout = 60.seconds) {
            val deps = FakeSettingsDependencies()
            withProfile(deps) { model ->
                var typedAhead = false
                show {
                    val ui by model.ui.collectAsStateWithLifecycle()
                    val field = rememberProfileNameField(model, ui.editor)
                    SideEffect {
                        if (ui.editor?.nameReplacement != null && !typedAhead) {
                            typedAhead = true
                            field.setTextAndPlaceCursorAtEnd("My new draft")
                        }
                    }
                }
                compose.runOnIdle {
                    deps.account.value = deps.account.value!!.copy(name = "Pulled")
                    runCurrent()
                }
                compose.waitForIdle()
                compose.runOnIdle { runCurrent() }
                assertTrue(typedAhead)
                assertEquals(
                    "My new draft",
                    model.ui.value.editor
                        ?.name,
                )
            }
        }
}
