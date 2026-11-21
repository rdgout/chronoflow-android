@file:OptIn(androidx.compose.runtime.InternalComposeTracingApi::class)

package net.chronoflow.feature.settings.home

import androidx.compose.runtime.Composer
import androidx.compose.runtime.CompositionTracer
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import kotlinx.coroutines.flow.emptyFlow
import net.chronoflow.core.plan.PlanClock
import net.chronoflow.core.testing.FakePlanClock
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.settings.support.SettingsScreenTest
import net.chronoflow.feature.settings.support.settingsNow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.ZoneOffset

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsHomeScreenTest : SettingsScreenTest() {
    private val clock = FakePlanClock(settingsNow)

    private fun state(
        ui: SettingsHomeUi =
            SettingsHomeUi(
                SettingsHomeState(
                    account = account(syncedAt = settingsNow.minusSeconds(120).toString()),
                    missingAnswers = 3,
                    credits = 2,
                ),
            ),
        showUnbuilt: Boolean = true,
    ) = SettingsHomeScreenState(ui, clock, ZoneOffset.UTC, showUnbuilt = showUnbuilt)

    @Test fun rowsAndBadgesUseLocalValues() {
        show { SettingsHomeScreen(state(), {}) }
        compose.onNodeWithTag("settings.readiness", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("settings.plan").assertExists()
        compose.onNodeWithTag("settings.account").assertExists()
        compose.onNodeWithTag("settings.profile").assertExists()
    }

    @Test fun releaseOnlyShowsBuiltDestinations() {
        show { SettingsHomeScreen(state(showUnbuilt = false), {}) }
        compose.onNodeWithTag("settings.account").assertExists()
        compose.onNodeWithTag("settings.security").assertDoesNotExist()
        compose.onNodeWithTag("settings.bodyClock").assertExists()
        compose.onNodeWithTag("settings.preferences").assertExists()
        compose.onNodeWithTag("settings.plan").assertDoesNotExist()
    }

    @Test fun busyDisablesSignOut() {
        show { SettingsHomeScreen(state(SettingsHomeUi(SettingsHomeState(account = account()), busy = true)), {}) }
        compose.onNodeWithTag("settings.signOut").assertIsNotEnabled()
    }

    @Test fun noAccountHidesTheProfileAndDisablesSignOut() {
        show { SettingsHomeScreen(state(SettingsHomeUi()), {}) }
        compose.onNodeWithTag("settings.signOut").assertIsNotEnabled()
        compose.onNodeWithTag("settings.profile").assertDoesNotExist()
    }

    @Test fun minuteTickOnlyRecomposesTheSyncLine() {
        var parentCompositions = 0
        var home = 0
        var line = 0
        var rows = 0
        val tracer =
            object : CompositionTracer {
                override fun isTraceInProgress() = true

                override fun traceEventStart(
                    key: Int,
                    dirty1: Int,
                    dirty2: Int,
                    info: String,
                ) {
                    if (info.startsWith("net.chronoflow.feature.settings.home.SettingsHomeScreen ")) home++
                    if (info.startsWith("net.chronoflow.feature.settings.home.SettingsSyncLine ")) line++
                    if (info.startsWith("net.chronoflow.feature.settings.components.SettingsDestinationRow ")) rows++
                }

                override fun traceEventEnd() = Unit
            }
        Composer.setTracer(tracer)
        try {
            val initial = state()
            show {
                SideEffect { parentCompositions++ }
                SettingsHomeScreen(initial, {})
            }
            compose.runOnIdle {
                assertTrue(home > 0 && line > 0 && rows > 0)
                home = 0
                line = 0
                rows = 0
                clock.advance(settingsNow.plusSeconds(60))
            }
            compose.runOnIdle {
                assertEquals(1, parentCompositions)
                assertEquals(0, home)
                assertEquals(0, rows)
                assertTrue(line > 0)
            }
        } finally {
            Composer.setTracer(null)
        }
    }

    @Test fun firstCompositionUsesTheCurrentInstantBeforeTheFirstTick() {
        val initialClock =
            object : PlanClock {
                override val now = settingsNow

                override fun values() = emptyFlow<java.time.Instant>()
            }
        val state = SettingsHomeState(account = account(syncedAt = settingsNow.minusSeconds(120).toString()))
        val observed = mutableListOf<SyncLine?>()
        show {
            val line = rememberSettingsSyncLine(state, false, false, initialClock, ZoneOffset.UTC)
            SideEffect { observed += line }
        }
        assertEquals(SyncLine.Minutes(2), observed.first())
        assertTrue(SyncLine.JustNow !in observed)
    }

    @Test fun oldSyncDateIsPresentBeforeTheCopyCatalogLoads() {
        val state = SettingsHomeState(account = account(syncedAt = settingsNow.minusSeconds(8 * 86400).toString()))
        show { SettingsSyncLine(state, false, false, clock, ZoneOffset.UTC, formatter = null) }
        compose.onNodeWithTag("settings.sync").assertExists()
    }
}
