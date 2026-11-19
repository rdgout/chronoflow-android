package net.chronoflow.feature.auth.verify

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.chronoflow.core.designsystem.component.CfButton
import net.chronoflow.core.designsystem.component.CfButtonSize
import net.chronoflow.core.designsystem.component.CfButtonStyle
import net.chronoflow.core.designsystem.effect.cfClickable
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.ui.R
import net.chronoflow.feature.auth.model.AuthFormContext
import net.chronoflow.feature.auth.model.AuthFormModel
import net.chronoflow.feature.auth.model.AuthFormState
import net.chronoflow.feature.auth.model.VerifyEmailModel
import net.chronoflow.feature.auth.model.VerifyState
import net.chronoflow.feature.auth.model.VerifyUiState
import net.chronoflow.feature.auth.shell.AuthBanner
import net.chronoflow.feature.auth.shell.AuthFailureFeedback
import net.chronoflow.feature.auth.shell.AuthHeadingGlyph
import net.chronoflow.feature.auth.shell.AuthMetrics
import net.chronoflow.feature.auth.shell.AuthScreen
import net.chronoflow.feature.auth.shell.resolve
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

data class VerifyEmailUiState(
    val email: String,
    val state: VerifyState = VerifyState.Waiting,
    val form: AuthFormState = AuthFormState(),
    val signOutFailure: DataFailure? = null,
    val canResend: Boolean = false,
)

internal fun VerifyUiState.presentation(email: String): VerifyEmailUiState =
    VerifyEmailUiState(email, state, form, signOutFailure, canResend)

enum class VerifyEmailEvent { OpenEmail, Resend, Retry, SignOut }

private class VerifyRequest {
    var job: Job? = null
}

@Composable
fun VerifyEmailRoute(
    model: VerifyEmailModel,
    openEmailApp: () -> Boolean,
) {
    val presentation =
        remember(model) { model.uiState.map { it.presentation(model.context.email) }.distinctUntilChanged() }
    val initialPresentation = remember(model) { model.uiState.value.presentation(model.context.email) }
    val state by presentation.collectAsStateWithLifecycle(initialPresentation)
    val remainingState =
        remember(model) { model.uiState.map { it.remaining }.distinctUntilChanged() }
            .collectAsStateWithLifecycle(model.remaining)
    val remaining = remember(remainingState) { { remainingState.value } }
    val resources = LocalResources.current
    val request = remember(model) { VerifyRequest() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(model, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            withContext(model.scope.coroutineContext.minusKey(Job)) {
                model.activate()
                while (isActive) {
                    delay(1.seconds)
                    model.tick()
                }
            }
        }
    }
    DisposableEffect(model) {
        onDispose {
            request.job?.cancel()
            model.cancel()
        }
    }
    VerifyEmailScreen(state, remaining, onEvent = { event ->
        if (request.job?.isActive != true && !model.form.busy) {
            val job =
                model.scope.launch(start = CoroutineStart.LAZY) {
                    try {
                        when (event) {
                            VerifyEmailEvent.OpenEmail -> model.openEmail { openEmailApp() }
                            VerifyEmailEvent.Resend -> model.resend()
                            VerifyEmailEvent.Retry -> model.check()
                            VerifyEmailEvent.SignOut -> model.signOut()
                        }
                    } finally {
                        if (request.job === currentCoroutineContext()[Job]) request.job = null
                    }
                }
            request.job = job
            job.start()
        }
    }, takeAnnouncement = { model.takeAnnouncement { it.resolve(resources) } })
}

@Composable
fun VerifyEmailScreen(
    state: VerifyEmailUiState,
    remaining: () -> Int,
    onEvent: (VerifyEmailEvent) -> Unit,
    modifier: Modifier = Modifier,
    takeAnnouncement: () -> String? = { null },
) {
    VerifyEmailContent(state, remaining, onEvent, modifier, takeAnnouncement)
}

@Composable
fun VerifyEmailContent(
    state: VerifyEmailUiState,
    remaining: () -> Int,
    onEvent: (VerifyEmailEvent) -> Unit,
    modifier: Modifier = Modifier,
    takeAnnouncement: () -> String? = { null },
) {
    val failed = state.state == VerifyState.Failed
    val enabled = !state.form.busy
    AuthScreen(
        scrollTag = "verify.scroll",
        title =
            stringResource(
                if (failed) R.string.auth_we_couldn_t_check_your_email else R.string.auth_check_your_inbox,
            ),
        description =
            if (failed) {
                stringResource(
                    R.string.auth_the_link_may_have_expired_or_we_coul_7bd9a7,
                )
            } else {
                stringResource(R.string.auth_we_sent_a_link_to_x_open_it_on_this_694644, state.email)
            },
        onBack = { onEvent(VerifyEmailEvent.SignOut) },
        modifier = modifier,
        headingGlyph = if (failed) AuthHeadingGlyph.Problem else null,
        enabled = enabled,
        announcementRevision = state.form.announcementRevision,
        takeAnnouncement = takeAnnouncement,
    ) {
        val banner = remember { BringIntoViewRequester() }
        AuthFailureFeedback(state.form, emptyList(), banner)
        if (state.signOutFailure != null) {
            Text(
                stringResource(signOutErrorResource(state.signOutFailure)),
                Modifier.testTag("verify.sign-out-error").bringIntoViewRequester(banner),
                color = CfTheme.colors.destructive,
                style = CfTheme.type.authBody,
            )
        } else {
            state.form.banner?.let { AuthBanner(it, Modifier.bringIntoViewRequester(banner), AuthFormContext.Verify) }
        }
        VerifyEmailButtons(state, onEvent)
        if (!failed) {
            VerifyCountdown(remaining)
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = AuthMetrics.TOUCH.dp)
                    .testTag("verify.different-email")
                    .cfClickable(onClick = { onEvent(VerifyEmailEvent.SignOut) }, enabled = enabled, role = Role.Button)
                    .padding(horizontal = AuthMetrics.DIFFERENT_EMAIL_HORIZONTAL.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.auth_use_a_different_email),
                    color = CfTheme.colors.foreground,
                    style = CfTheme.type.authLabel.copy(textDecoration = TextDecoration.Underline),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun VerifyEmailButtons(
    state: VerifyEmailUiState,
    onEvent: (VerifyEmailEvent) -> Unit,
) {
    val failed = state.state == VerifyState.Failed
    val enabled = !state.form.busy
    Column(
        Modifier.verifyButtons(state).fillMaxWidth(),
        verticalArrangement =
            Arrangement.spacedBy(
                if (failed) AuthTokens.cardGap.dp else AuthMetrics.VERIFY_BUTTON_GAP.dp,
            ),
    ) {
        val primary = stringResource(if (failed) R.string.auth_try_again else R.string.auth_open_email_app)
        CfButton(
            primary,
            { onEvent(if (failed) VerifyEmailEvent.Retry else VerifyEmailEvent.OpenEmail) },
            Modifier.testTag(if (failed) "verify.retry" else "verify.open-email"),
            size = CfButtonSize.Lg,
            fillWidth = true,
            enabled = enabled,
            minHeight = if (failed) AuthMetrics.FAILED_BUTTON_MIN_HEIGHT.dp else Dp.Unspecified,
            loading = failed && state.form.pending == AuthFormModel.Action.CheckVerification,
            loadingDescription = primary,
        )
        val resend = stringResource(if (failed) R.string.auth_send_a_new_link else R.string.auth_resend_link)
        CfButton(
            resend,
            { onEvent(VerifyEmailEvent.Resend) },
            Modifier.testTag("verify.resend"),
            style = CfButtonStyle.Outline,
            size = CfButtonSize.Lg,
            fillWidth = true,
            enabled = state.canResend,
            minHeight = if (failed) AuthMetrics.FAILED_BUTTON_MIN_HEIGHT.dp else Dp.Unspecified,
            loading = state.form.pending == AuthFormModel.Action.ResendVerification,
            loadingDescription = resend,
        )
    }
}

@Composable
private fun VerifyCountdown(remaining: () -> Int) {
    val seconds = remaining()
    if (seconds > 0) {
        Text(
            stringResource(R.string.auth_you_can_resend_in_x, countdownArgument(seconds)),
            Modifier.verifyCountdown(seconds).fillMaxWidth().testTag("verify.countdown"),
            color = CfTheme.colors.mutedForeground,
            style = CfTheme.type.authHint.copy(fontFeatureSettings = "tnum"),
            textAlign = TextAlign.Center,
        )
    }
}

internal fun countdownArgument(seconds: Int): String = String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60)

internal fun signOutErrorResource(failure: DataFailure): Int =
    if (failure == DataFailure.Offline) R.string.auth_needs_a_connection else R.string.auth_something_went_wrong
