package net.chronoflow.app.guest

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalAutofillManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.model.api.AuthLanguage
import net.chronoflow.feature.auth.guest.GuestSignupEvent
import net.chronoflow.feature.auth.guest.GuestSignupFields
import net.chronoflow.feature.auth.guest.GuestSignupSheet
import net.chronoflow.feature.auth.guest.GuestSignupUiState
import net.chronoflow.feature.auth.shell.TextFieldSecret
import net.chronoflow.feature.auth.shell.resolve
import net.chronoflow.feature.auth.signup.CustomTabPolicyOpener

@Composable
internal fun GuestSignupRoute(
    shown: GuestSignupPresentation,
    coordinator: GuestSignupCoordinator,
) {
    val model = shown.model
    val name by model.name.collectAsStateWithLifecycle()
    val email by model.email.collectAsStateWithLifecycle()
    val form by model.form.state.collectAsStateWithLifecycle()
    val guest by model.guestState.collectAsStateWithLifecycle()
    val fields =
        remember(model) {
            GuestSignupFields(model.name.value, model.email.value).also { shown.prefill?.fillSecrets(it) }
        }
    val password = remember(fields) { TextFieldSecret(fields.password) }
    val confirmation = remember(fields) { TextFieldSecret(fields.confirmation) }
    val context = LocalContext.current
    val dark = CfTheme.colors.isDark
    val opener = remember(context, dark) { CustomTabPolicyOpener(context, dark) }
    val resources = LocalResources.current
    val autofill = LocalAutofillManager.current
    DisposableEffect(fields) { onDispose { fields.clearSecrets() } }
    GuestSignupSheet(GuestSignupUiState(name, email, form, guest), fields, opener, onEvent = {
        when (it) {
            is GuestSignupEvent.EditName -> {
                model.editName(it.value)
            }

            is GuestSignupEvent.EditEmail -> {
                model.editEmail(it.value)
            }

            GuestSignupEvent.EditPassword -> {
                model.editPassword()
            }

            GuestSignupEvent.EditConfirmation -> {
                model.editConfirmation()
            }

            GuestSignupEvent.Submit -> {
                coordinator.submit(
                    password,
                    confirmation,
                    if (resources.configuration.locales[0].language ==
                        "nl"
                    ) {
                        AuthLanguage.Nl
                    } else {
                        AuthLanguage.En
                    },
                ) { autofill?.commit() }
            }

            GuestSignupEvent.SignInInstead -> {
                coordinator.signInInstead()
            }

            GuestSignupEvent.Review -> {
                coordinator.review()
            }

            GuestSignupEvent.Dismiss -> {
                coordinator.dismiss()
            }
        }
    }, takeAnnouncement = { model.takeAnnouncement { it.resolve(resources) } })
}
