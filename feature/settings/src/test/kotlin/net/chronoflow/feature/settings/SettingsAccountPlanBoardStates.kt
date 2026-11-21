package net.chronoflow.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalResources
import kotlinx.serialization.json.JsonObject
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.api.ApiPasskey
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.ui.l10n.generated.CopyIndex
import net.chronoflow.core.ui.l10n.generated.CopyTable
import net.chronoflow.feature.settings.account.AccountScreen
import net.chronoflow.feature.settings.account.AccountScreenState
import net.chronoflow.feature.settings.components.settingsFormatArguments
import net.chronoflow.feature.settings.delete.DeleteAccountInputs
import net.chronoflow.feature.settings.delete.DeleteAccountScreenState
import net.chronoflow.feature.settings.delete.DeleteAccountSheet
import net.chronoflow.feature.settings.delete.DeleteAccountUiState
import net.chronoflow.feature.settings.delete.DeleteState
import net.chronoflow.feature.settings.delete.SettingsDeleteModel
import net.chronoflow.feature.settings.passkeys.PasskeysInputs
import net.chronoflow.feature.settings.passkeys.PasskeysScreen
import net.chronoflow.feature.settings.passkeys.PasskeysScreenState
import net.chronoflow.feature.settings.passkeys.SettingsPasskeyText
import net.chronoflow.feature.settings.plan.PlanScreen
import net.chronoflow.feature.settings.plan.PlanScreenState
import net.chronoflow.feature.settings.plan.SettingsPlanState
import net.chronoflow.feature.settings.plan.SettingsPlanText
import net.chronoflow.feature.settings.security.SecurityState
import net.chronoflow.feature.settings.security.SettingsSecurityPrompt
import net.chronoflow.feature.settings.support.SettingsFixture
import java.time.Instant
import java.time.ZoneOffset

internal object SettingsAccountPlanBoardStates {
    val now: Instant = Instant.parse("2026-10-31T12:00:00Z")
    val passkeys =
        listOf(
            ApiPasskey(1, "iPhone 16", null, "2026-09-14T08:00:00Z", "2026-10-31T08:00:00Z"),
            ApiPasskey(2, "MacBook Pro", null, "2026-10-02T08:00:00Z", "2026-10-28T08:00:00Z"),
        )
    val states =
        listOf(
            "PasskeysLoading",
            "PasskeysList",
            "PasskeysLong",
            "PasskeysEmpty",
            "PasskeysUnavailable",
            "PasskeysOffline",
            "PasskeysAdd",
            "PasskeysRemove",
            "DeleteConfirmation",
            "DeletePrompt",
            "DeleteWrong",
            "DeleteLocked",
            "PlanLoading",
            "PlanCredits",
            "PlanUnlimited",
            "PlanFailed",
            "PlanOffline",
            "PlanMissing",
        )
}

@Composable
internal fun SettingsAccountPlanBoard(state: String) {
    val now = SettingsAccountPlanBoardStates.now
    val resources = LocalResources.current
    val language = Language.of(resources.configuration.locales[0].language)
    val formatter = remember { CopyLineFormatter(SettingsFixture.catalog) }
    val clock = remember { { now } }
    when {
        state.startsWith("Passkeys") -> {
            val keys =
                if (state == "PasskeysLong") {
                    List(6) { index ->
                        ApiPasskey(
                            index.toLong() + 1,
                            "A long passkey name for Rick's travelling laptop $index",
                            null,
                            "2026-09-14T08:00:00Z",
                            "2026-10-31T08:00:00Z",
                        )
                    }
                } else {
                    SettingsAccountPlanBoardStates.passkeys
                }
            val prompt =
                when (state) {
                    "PasskeysAdd" -> SettingsSecurityPrompt.AddPasskey
                    "PasskeysRemove" -> SettingsSecurityPrompt.RemovePasskey
                    else -> null
                }
            val security =
                SecurityState(
                    passkeys =
                        when (state) {
                            "PasskeysLoading", "PasskeysUnavailable" -> null
                            "PasskeysEmpty" -> emptyList()
                            else -> keys
                        },
                    passkeysUnavailable = state == "PasskeysUnavailable",
                    offline = state == "PasskeysOffline",
                    prompt = prompt,
                    removal = keys.first().takeIf { state == "PasskeysRemove" },
                )
            val meta =
                remember(keys, language, resources) {
                    keys
                        .mapNotNull { key ->
                            SettingsPasskeyText
                                .meta(key, language, now, ZoneOffset.UTC, formatter) { key, args ->
                                    val entry = checkNotNull(CopyIndex.find(CopyTable.Settings, key))
                                    resources.getString(entry.resId, *settingsFormatArguments(entry.args, args))
                                }?.let { key.id to it }
                        }.toMap()
                }
            val inputs = remember { PasskeysInputs() }
            PasskeysScreen(PasskeysScreenState(security, meta, inputs = inputs, now = clock), {})
        }

        state.startsWith("Delete") -> {
            AccountScreen(AccountScreenState(SettingsPagesState(SettingsBoardFixture.account, loaded = true)), {})
            val inputs = remember { DeleteAccountInputs() }
            val stage =
                if (state == "DeleteConfirmation") {
                    SettingsDeleteModel.Stage.Confirmation
                } else {
                    SettingsDeleteModel.Stage.Password
                }
            val delete =
                DeleteState(
                    stage,
                    errorKey = "The provided password was incorrect.".takeIf { state == "DeleteWrong" },
                    retryAt = now.plusSeconds(2820).takeIf { state == "DeleteLocked" },
                )
            DeleteAccountSheet(
                DeleteAccountScreenState(
                    DeleteAccountUiState(delete, email = "rick@example.com"),
                    inputs,
                    clock,
                ),
                {},
            )
        }

        else -> {
            val unlimited = state in setOf("PlanUnlimited", "PlanFailed", "PlanOffline")
            val credits =
                if (state == "PlanMissing") {
                    null
                } else {
                    ApiJson.response.parseToJsonElement(
                        if (unlimited) """{"unlimited":true}""" else """{"credits":2}""",
                    ) as JsonObject
                }
            val billing =
                ApiJson.response.parseToJsonElement(
                    """{"subscription":{"tier":"annual",
                "canResume":${state == "PlanOffline"},"paymentFailed":${state == "PlanFailed"},
                "renewsAt":"2026-11-14T00:00:00Z","endsAt":"2026-11-14T00:00:00Z"}}""",
                ) as JsonObject
            val plan = SettingsPlanState(credits, billing, state != "PlanLoading", state == "PlanOffline")
            PlanScreen(PlanScreenState(plan, SettingsPlanText.line(plan, formatter, language, ZoneOffset.UTC)), {})
        }
    }
}
