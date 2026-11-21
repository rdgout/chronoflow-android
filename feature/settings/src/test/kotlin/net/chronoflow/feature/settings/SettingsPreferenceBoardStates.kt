package net.chronoflow.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.datamock.MockAppSeed
import net.chronoflow.core.datamock.MockScenarioValues
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.generated.SettingsTokens
import net.chronoflow.core.designsystem.layout.LocalSafeInsets
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.model.contracts.ProfilePreferences
import net.chronoflow.feature.settings.bodyclock.BodyClockGroup
import net.chronoflow.feature.settings.bodyclock.BodyClockReadiness
import net.chronoflow.feature.settings.bodyclock.BodyClockSection
import net.chronoflow.feature.settings.bodyclock.BodyClockUiState
import net.chronoflow.feature.settings.bodyclock.BodyClockViewModel
import net.chronoflow.feature.settings.bodyclock.SettingsBodyClockModel
import net.chronoflow.feature.settings.preferences.PreferencesUiState
import net.chronoflow.feature.settings.preferences.SettingsPreferencesModel
import net.chronoflow.feature.settings.preferences.SettingsTimelineGroup

internal object SettingsPreferenceBoardStates {
    private val snapshot by lazy {
        MockAppSeed
            .make(SettingsBoardFixture.now)
            .accounts
            .getValue(MockScenarioValues.demoID)
            .snapshot
    }

    fun preferences() =
        PreferencesUiState(
            editor =
                SettingsPreferencesModel.State(
                    JsonObject(
                        mapOf(
                            "timeFormat" to JsonPrimitive("24"),
                            "hiddenCategories" to JsonArray(SettingsTimelineGroup.all[3].types.map(::JsonPrimitive)),
                        ),
                    ),
                ),
        )

    fun bodyClock(state: String): BodyClockUiState {
        val body = snapshot.settings.bodyClock.toMutableMap()
        when (state.substringBefore('-')) {
            "bodyMissing" -> {
                body["ageBand"] = JsonNull
                body["shiftWork"] = JsonNull
            }

            "bodyAdjusted" -> {
                body["ageBand"] = JsonPrimitive("65_plus")
                body["excludeCaffeine"] = JsonPrimitive(true)
                body["flightMovementBreaks"] = JsonPrimitive(true)
            }

            "bodyExcluded" -> {
                body["shiftWork"] = JsonPrimitive(true)
            }
        }
        val answers = ProfilePreferences.bodyClock(JsonObject(body))
        val readiness = ProfilePreferences.readiness(JsonObject(emptyMap()), answers)
        val editor = SettingsBodyClockModel.State(answers)
        return BodyClockUiState(
            pages = SettingsPagesState(settingsLoaded = true),
            editor = editor,
            sections = BodyClockViewModel.sections(editor, BodyClockReadiness.missingFields(readiness).toSet()),
            readiness = BodyClockReadiness.of(readiness),
        )
    }
}

@Composable
internal fun SettingsBodySectionCapture(
    state: String,
    group: BodyClockGroup,
) {
    val ui = remember(state) { SettingsPreferenceBoardStates.bodyClock(state) }
    val rating = remember { TextFieldState() }
    val requesters =
        remember { BodyClockGroup.entries.flatMap { it.fields }.associateWith { BringIntoViewRequester() } }
    Box(
        Modifier
            .fillMaxSize()
            .background(CfTheme.colors.background)
            .windowInsetsPadding(LocalSafeInsets.current)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = PlanUITokens.space4.dp)
            .padding(top = PlanUITokens.space1.dp, bottom = SettingsTokens.pageBottom.dp),
    ) {
        BodyClockSection(ui.sections.getValue(group), rating, requesters, {})
    }
}
