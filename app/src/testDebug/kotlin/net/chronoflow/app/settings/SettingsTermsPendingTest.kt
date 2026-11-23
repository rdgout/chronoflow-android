package net.chronoflow.app.settings

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import kotlinx.coroutines.flow.MutableStateFlow
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.api.AccountPolicies
import net.chronoflow.core.model.store.OutboxRecord
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.onboarding.terms.TermsEvent
import net.chronoflow.feature.onboarding.terms.TermsScreen
import net.chronoflow.feature.onboarding.terms.TermsUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = android.app.Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsTermsPendingTest {
    private val compose = createEmptyComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)

    @Test fun termsSlotShowsDistinctEntriesAndStaysWhenTheQueueEmpties() {
        org.robolectric.nativeruntime.DefaultNativeRuntimeLoader
            .injectAndLoad()
        android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
        val host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val rows =
            MutableStateFlow(
                listOf(
                    OutboxRecord("1", "settings", """{"section":"bodyClock","patch":{"habitual_bed":"23:00"}}""", "1"),
                    OutboxRecord("2", "settings", """{"section":"bodyClock","patch":{"habitual_bed":"23:00"}}""", "2"),
                    OutboxRecord("3", "profile", """{"name":"Rick","email":"r@example.com"}""", "3"),
                ),
            )
        var event: TermsEvent? = null
        try {
            host.get().setContent {
                ProfileHost(DeviceProfiles.pixel8) {
                    ChronoFlowTheme(reduceMotion = true) {
                        TermsScreen(
                            TermsUiState(policies = AccountPolicies("1", "1"), pendingSignOutCount = 3),
                            {
                                event =
                                    it
                            },
                            signOutConfirmation = {
                                count,
                                stay,
                                discard,
                                ->
                                TermsSignOutConfirmation(count, rows, stay, discard)
                            },
                        )
                    }
                }
            }
            compose.onNodeWithTag("settings.pending").assertExists()
            compose.onNodeWithTag("settings.pending.item.1").assertExists()
            compose.onNodeWithTag("settings.pending.item.2").assertDoesNotExist()
            compose.onNodeWithTag("settings.pending.item.3").assertExists()
            compose.runOnIdle { rows.value = emptyList() }
            compose.onNodeWithTag("settings.pending").assertDoesNotExist()
            assertEquals(TermsEvent.Stay, event)
        } finally {
            host.pause().stop().destroy()
        }
    }
}
