package net.chronoflow.app.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OverlayViewModelTest {
    @get:Rule val compose = createComposeRule()

    private class Model(
        val number: Int,
    ) : ViewModel()

    @Test fun theExitCompositionCannotRecreateAClearedViewModel() {
        val first = TabViewModelStoreOwner()
        val owner = mutableStateOf(first)
        val tick = mutableIntStateOf(0)
        var creations = 0
        var shown = 0
        compose.setContent {
            val frame = tick.intValue
            CompositionLocalProvider(LocalViewModelStoreOwner provides owner.value) {
                val factory = viewModelFactory { initializer { Model(++creations) } }
                val model: Model = rememberOverlayViewModel("wizard", factory, CreationExtras.Empty)
                SideEffect { shown = model.number }
                Box(Modifier.testTag("frame.$frame"))
            }
        }
        compose.runOnIdle {
            assertEquals(1, creations)
            first.viewModelStore.clear()
            tick.intValue++
        }
        compose.runOnIdle {
            assertEquals(1, creations)
            assertEquals(1, shown)
        }
        val next = TabViewModelStoreOwner()
        compose.runOnIdle { owner.value = next }
        compose.runOnIdle {
            assertEquals(2, creations)
            assertEquals(2, shown)
            next.viewModelStore.clear()
        }
    }
}
