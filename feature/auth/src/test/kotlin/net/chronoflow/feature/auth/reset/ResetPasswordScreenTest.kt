package net.chronoflow.feature.auth.reset

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.auth.model.ResetPasswordModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class ResetPasswordScreenTest {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)
    private val state = mutableStateOf(ResetPasswordUiState())

    private fun host(
        onEvent: (ResetPasswordEvent) -> Unit = {},
        take: () -> String? = { null },
    ) {
        compose.setContent {
            ProfileHost(DeviceProfiles.pixel8) {
                ChronoFlowTheme(darkTheme = false) {
                    ResetPasswordScreen(
                        state.value,
                        remember {
                            TextFieldState()
                        },
                        remember { TextFieldState() },
                        onEvent,
                        takeAnnouncement = take,
                    )
                }
            }
        }
    }

    @Test fun nextFocusesConfirmationAndGoEmitsSubmit() {
        var submits = 0
        host(onEvent = { if (it == ResetPasswordEvent.Submit) submits++ })
        compose.onNodeWithTag("reset.password").performTextInput("NewPassword12!")
        compose.onNodeWithTag("reset.password").performImeAction()
        compose.onNodeWithTag("reset.confirmation").assertIsFocused().performTextInput("NewPassword12!")
        compose.onNodeWithTag("reset.confirmation").performImeAction()
        assertEquals(1, submits)
    }

    @Test fun bothFieldsCarryTheNewPasswordHint() {
        host()
        assertEquals(
            ContentType.NewPassword,
            compose.onNodeWithTag("reset.password").fetchSemanticsNode().config[SemanticsProperties.ContentType],
        )
        assertEquals(
            ContentType.NewPassword,
            compose.onNodeWithTag("reset.confirmation").fetchSemanticsNode().config[SemanticsProperties.ContentType],
        )
    }

    @Test fun formAndDoneHideDrawnBackAndKeepTheBrandCentred() {
        host()
        assertBrand()
        compose.runOnIdle { state.value = state.value.copy(state = ResetPasswordModel.State.Done) }
        assertBrand()
        compose.onNodeWithTag("reset.password").assertDoesNotExist()
        compose.onNodeWithTag("reset.continue").assertExists()
    }

    private fun assertBrand() {
        compose.onNodeWithTag("auth.back").assertDoesNotExist()
        val brand = compose.onNodeWithTag("auth.brand").fetchSemanticsNode().boundsInRoot
        assertEquals(
            DeviceProfiles.pixel8.widthDp * DeviceProfiles.pixel8.density / 2,
            brand.center.x,
            0.5f * DeviceProfiles.pixel8.density,
        )
    }

    @Test fun doneAnnouncesOnceAndContinueEmitsBack() {
        var announcements = 0
        var backs = 0
        host(onEvent = { if (it == ResetPasswordEvent.Back) backs++ }, take = {
            if (state.value.state == ResetPasswordModel.State.Done) {
                announcements++
                ""
            } else {
                null
            }
        })
        compose.runOnIdle { state.value = state.value.copy(state = ResetPasswordModel.State.Done) }
        compose.onNodeWithTag("reset.continue").assertExists()
        compose.runOnIdle { state.value = state.value.copy(form = state.value.form.copy(retryAfterSeconds = 1.0)) }
        compose.onNodeWithTag("reset.continue").performClick()
        assertEquals(1, announcements)
        assertEquals(1, backs)
    }
}
