package net.chronoflow.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.system.CfSnackbarHost
import net.chronoflow.core.model.api.ApiTwoFactorSetup
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.feature.settings.security.PasswordInputs
import net.chronoflow.feature.settings.security.PasswordScreen
import net.chronoflow.feature.settings.security.PasswordScreenState
import net.chronoflow.feature.settings.security.QrMatrix
import net.chronoflow.feature.settings.security.SecurityScreen
import net.chronoflow.feature.settings.security.SecurityScreenState
import net.chronoflow.feature.settings.security.SecurityState
import net.chronoflow.feature.settings.security.SecuritySummary
import net.chronoflow.feature.settings.security.SecurityUiState
import net.chronoflow.feature.settings.security.SettingsSecurityPrompt
import net.chronoflow.feature.settings.security.TwoFactorInputs
import net.chronoflow.feature.settings.security.TwoFactorScreen
import net.chronoflow.feature.settings.security.TwoFactorScreenState
import net.chronoflow.feature.settings.security.securityText
import net.chronoflow.feature.settings.support.SettingsFixture
import java.time.Instant

internal object SettingsSecurityBoardStates {
    val now: Instant = Instant.parse("2026-10-30T12:00:00Z")
    val setup =
        ApiTwoFactorSetup(
            "JBSWY3DPEHPK3PXP",
            "otpauth://totp/ChronoFlow?secret=JBSWY3DPEHPK3PXP&issuer=ChronoFlow",
        )
    val codes =
        listOf(
            "8f3k-29dm",
            "q7zp-4wxr",
            "m2vt-8ncb",
            "h6ye-1lsa",
            "c9rk-5jtd",
            "w4bn-7ugq",
            "t1xf-3hzo",
            "p5ma-6evk",
        )
    val qr by lazy { QrMatrix.encode(setup.otpauthUrl) }
    val states =
        listOf(
            "Security",
            "Password",
            "PasswordError",
            "PasswordLocked",
            "Prompt",
            "PromptEnable",
            "PromptChecking",
            "PromptWrong",
            "PromptRegenerateWrong",
            "PromptLocked",
            "PromptForgot",
            "PromptFailure",
            "PromptRegenerate",
            "TwoFactorIntro",
            "TwoFactorSetup",
            "TwoFactorFailed",
            "TwoFactorManage",
            "TwoFactorCodes",
            "TwoFactorOn",
        )

    fun handles(state: String) = state in states
}

@Composable
internal fun SettingsSecurityBoard(state: String) {
    val now = SettingsSecurityBoardStates.now
    val nowProvider = remember { { now } }
    val locale =
        LocalResources.current.configuration.locales[0]
            .language
    val passwordInputs =
        remember(state) {
            PasswordInputs().apply {
                if (state == "PasswordError") {
                    current.edit { append("currentpass") }
                    new.edit { append("newpassword1") }
                    confirmation.edit { append("newpasword1") }
                }
            }
        }
    val inputs = remember { TwoFactorInputs() }
    val summary = SecuritySummary(false, "2026-10-30T08:00:00Z", 2)
    if (state == "Security") {
        SecurityScreen(SecurityScreenState(summary, loaded = true, showUnbuilt = true), {})
    } else if (state.startsWith("Password")) {
        val security =
            SecurityState(
                errors =
                    if (state ==
                        "PasswordError"
                    ) {
                        mapOf("password_confirmation" to "The passwords don't match.")
                    } else {
                        emptyMap()
                    },
                retryAt = if (state == "PasswordLocked") now.plusSeconds(2820) else null,
            )
        PasswordScreen(PasswordScreenState(security, passwordInputs, nowProvider), {})
    } else {
        val prompt =
            when (state) {
                "PromptEnable", "PromptChecking", "PromptForgot", "PromptFailure" -> SettingsSecurityPrompt.Enable
                "PromptWrong", "PromptRegenerateWrong", "PromptLocked" -> SettingsSecurityPrompt.Disable
                "PromptRegenerate", "PromptRegenerateWrong" -> SettingsSecurityPrompt.Regenerate
                "Prompt" -> SettingsSecurityPrompt.Reveal
                else -> null
            }
        val security =
            remember(state) {
                if (state == "PromptWrong" || state == "PromptRegenerateWrong" || state == "PromptChecking" ||
                    state == "PromptLocked"
                ) {
                    inputs.password.edit { append("password") }
                }
                SecurityState(
                    prompt = prompt,
                    setup =
                        SettingsSecurityBoardStates.setup.takeIf {
                            state == "TwoFactorSetup" ||
                                state == "TwoFactorFailed"
                        },
                    codes =
                        if (state == "TwoFactorCodes" ||
                            state == "TwoFactorOn"
                        ) {
                            SettingsSecurityBoardStates.codes
                        } else {
                            emptyList()
                        },
                    busy = state == "PromptChecking",
                    errors =
                        if (state == "PromptWrong" ||
                            state == "PromptRegenerateWrong"
                        ) {
                            mapOf("current_password" to "The provided password was incorrect.")
                        } else {
                            emptyMap()
                        },
                    retryAt = now.plusSeconds(2820).takeIf { state == "PromptLocked" },
                    message =
                        "Couldn't turn on two-factor.".takeIf {
                            state == "TwoFactorFailed" || state == "PromptFailure"
                        },
                )
            }
        val enabled =
            state == "TwoFactorManage" || state == "TwoFactorCodes" || state == "TwoFactorOn" ||
                prompt == SettingsSecurityPrompt.Reveal || prompt == SettingsSecurityPrompt.Regenerate ||
                prompt == SettingsSecurityPrompt.Disable
        val since =
            remember(locale) {
                CopyLineFormatter(SettingsFixture.catalog).formatDate(
                    "2026-10-30T08:00:00Z",
                    if (locale == "nl") Language.Nl else Language.En,
                )
            }
        val ui =
            SecurityUiState(
                SettingsPagesState(loaded = true, settingsLoaded = true),
                summary.copy(twoFactorEnabled = enabled),
                security,
                since = since,
            )
        val snackbar = remember { SnackbarHostState() }
        val notice = securityText("Two-factor is on. Save your recovery codes now.")
        LaunchedEffect(state, snackbar) {
            if (state == "TwoFactorOn") snackbar.showSnackbar(notice, duration = SnackbarDuration.Indefinite)
        }
        Box(Modifier.fillMaxSize()) {
            TwoFactorScreen(
                TwoFactorScreenState(
                    ui,
                    inputs,
                    nowProvider,
                    "rick@example.com",
                    SettingsSecurityBoardStates.qr,
                ),
                {},
            )
            CfSnackbarHost(
                snackbar,
                Modifier
                    .align(
                        Alignment.BottomCenter,
                    ).padding(PlanUITokens.space4.dp)
                    .testTag("settings.security.feedback"),
            )
        }
    }
}
