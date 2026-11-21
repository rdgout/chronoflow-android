package net.chronoflow.feature.settings.bodyclock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.component.CfCard
import net.chronoflow.core.designsystem.component.CfPillGroup
import net.chronoflow.core.designsystem.component.CfPillOption
import net.chronoflow.core.designsystem.component.CfTextField
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.model.contracts.int
import net.chronoflow.core.ui.R
import net.chronoflow.feature.settings.components.SettingsDrinkCounter
import net.chronoflow.feature.settings.components.SettingsError
import net.chronoflow.feature.settings.components.SettingsOption
import net.chronoflow.feature.settings.components.SettingsPreferenceSwitch
import net.chronoflow.feature.settings.components.SettingsQuestion
import net.chronoflow.feature.settings.components.SettingsRadioCards
import net.chronoflow.feature.settings.components.SettingsSavedLabel
import net.chronoflow.feature.settings.components.SettingsTimeTile
import net.chronoflow.feature.settings.components.SettingsTimeTilePair
import net.chronoflow.feature.settings.components.settingsText

@Composable
internal fun BodyClockSection(
    state: BodyClockSectionState,
    rating: TextFieldState,
    positions: BodyClockQuestionPositions,
    onEvent: (BodyClockEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    CfCard(modifier.onPlaced { positions.sectionPlaced(state.group, it) }.testTag("settings.bodyClock.section.${state.group.raw}")) {
        if (state.group == BodyClockGroup.Light) {
            Column(verticalArrangement = Arrangement.spacedBy(PlanUITokens.space1.dp)) {
                BodyGroupHeader(state)
                BodyCaption(R.string.settings_optional_if_you_have_any_of_thes_9b544f)
            }
        } else {
            BodyGroupHeader(state)
        }
        when (state.group) {
            BodyClockGroup.About -> BodyClockAbout(state, positions, onEvent)
            BodyClockGroup.Sleep -> BodyClockSleep(state, positions, onEvent)
            BodyClockGroup.Caffeine -> BodyClockCaffeine(state, positions, onEvent)
            BodyClockGroup.Light -> BodyClockLight(state, rating, positions, onEvent)
            BodyClockGroup.Health -> BodyClockHealth(state, positions, onEvent)
        }
    }
}

@Composable
private fun BodyGroupHeader(state: BodyClockSectionState) {
    Row(horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp)) {
        Text(
            settingsText(state.group.title),
            Modifier.weight(1f).alignByBaseline().semantics { heading() },
            style = CfTheme.type.style(CfTypeScale.screenSectionTitle),
        )
        if (state.saved) SettingsSavedLabel(state.group.raw, Modifier.alignByBaseline())
    }
}

@Composable
private fun ColumnScope.BodyClockAbout(
    state: BodyClockSectionState,
    positions: BodyClockQuestionPositions,
    onEvent: (BodyClockEvent) -> Unit,
) {
    Column(Modifier.onPlaced { positions.questionPlaced("chronotype", it) }, verticalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp)) {
        Text(
            stringResource(R.string.settings_when_do_you_naturally_feel_most_awake),
            style = CfTheme.type.style(CfTypeScale.settingsRow),
        )
        SettingsRadioCards(
            listOf(
                SettingsOption(
                    "early",
                    stringResource(R.string.settings_early_bird),
                    stringResource(R.string.settings_you_wake_early_without_an_alarm_f9b380),
                ),
                SettingsOption(
                    "intermediate",
                    stringResource(R.string.settings_in_between),
                    stringResource(R.string.settings_your_energy_follows_a_normal_wor_91ce51),
                ),
                SettingsOption(
                    "late",
                    stringResource(R.string.settings_night_owl),
                    stringResource(R.string.settings_you_come_alive_late_and_find_ear_2e439b),
                ),
                SettingsOption(
                    "",
                    stringResource(R.string.settings_not_sure),
                    stringResource(R.string.settings_we_use_your_sleep_times_instead),
                ),
            ),
            state.answers["chronotype"].stringValue().orEmpty(),
            "settings.bodyClock.chronotype",
            { onEvent(BodyClockEvent.Choice(state.group, "chronotype", it)) },
        )
    }
    BodyChoiceQuestion(
        state,
        "ageBand",
        R.string.settings_your_age,
        listOf(
            "under_18" to R.string.settings_under_18,
            "18_64" to R.string.settings_1864,
            "65_plus" to R.string.settings_65_or_older,
        ),
        positions,
        onEvent,
    )
}

@Composable
private fun ColumnScope.BodyClockSleep(
    state: BodyClockSectionState,
    positions: BodyClockQuestionPositions,
    onEvent: (BodyClockEvent) -> Unit,
) {
    BodyCaption(R.string.settings_on_a_normal_work_or_school_day)
    BodyTimePair(
        state,
        "habitualBed",
        R.string.settings_usual_bedtime,
        "habitualWake",
        R.string.settings_usual_wake_time,
        positions,
        onEvent,
    )
    BodyChoiceQuestion(
        state,
        "freeDayStatus",
        R.string.settings_do_your_free_days_follow_the_sam_b9315b,
        listOf("same_as_workdays" to R.string.settings_yes, "declared" to R.string.settings_no),
        positions,
        onEvent,
    )
    if (state.answers["freeDayStatus"].stringValue() == "declared") {
        Column(verticalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp)) {
            BodyCaption(R.string.settings_on_free_days)
            BodyTimePair(
                state,
                "freeDayBed",
                R.string.settings_bedtime,
                "freeDayWake",
                R.string.settings_wake_time,
                positions,
                onEvent,
            )
        }
    }
}

@Composable
private fun ColumnScope.BodyClockCaffeine(
    state: BodyClockSectionState,
    positions: BodyClockQuestionPositions,
    onEvent: (BodyClockEvent) -> Unit,
) {
    BodyChoiceQuestion(
        state,
        "caffeineHabit",
        R.string.settings_do_you_usually_drink_caffeine,
        listOf("cups" to R.string.settings_yes, "non_user" to R.string.settings_no),
        positions,
        onEvent,
    )
    if (state.answers["caffeineHabit"].stringValue() == "cups") {
        Column(verticalArrangement = Arrangement.spacedBy(PlanUITokens.space2.dp)) {
            SettingsDrinkCounter(
                SettingsBodyClockModel.cups.associateWith { state.answers[it].int ?: 0 },
                { field, count -> onEvent(BodyClockEvent.Drink(field, count)) },
            )
            state.errors["cupsFilterCoffee"]?.let {
                SettingsError(settingsText(it), Modifier.testTag("settings.bodyClock.cupsFilterCoffee.error"))
            }
        }
    }
    BodyChoiceQuestion(
        state,
        "caffeineSensitive",
        R.string.settings_does_caffeine_affect_you_strongly,
        yesNo(),
        positions,
        onEvent,
        boolean = true,
        hint = R.string.settings_for_example_one_cup_keeps_you_aw_4b60de,
    )
    SettingsPreferenceSwitch(
        stringResource(R.string.settings_leave_caffeine_out_of_my_plan),
        stringResource(R.string.settings_your_plan_won_t_suggest_any_caffeine),
        state.answers["excludeCaffeine"] == kotlinx.serialization.json.JsonPrimitive(true),
        { onEvent(BodyClockEvent.Preference(state.group, "excludeCaffeine", it)) },
        "settings.bodyClock.excludeCaffeine",
    )
}

@Composable
private fun ColumnScope.BodyClockLight(
    state: BodyClockSectionState,
    rating: TextFieldState,
    positions: BodyClockQuestionPositions,
    onEvent: (BodyClockEvent) -> Unit,
) {
    BodyChoiceQuestion(
        state,
        "lightDevice",
        R.string.settings_do_you_have_a_light_box_light_vi_ed7611,
        listOf(
            "none" to R.string.settings_none,
            "box" to R.string.settings_light_box_or_visor,
            "glasses" to R.string.settings_light_glasses,
        ),
        positions,
        onEvent,
    )
    BodyCaption(
        R.string.settings_don_t_use_a_light_box_or_light_g_d91df8,
        Modifier.testTag("settings.bodyClock.lightNotice"),
    )
    BodyChoiceQuestion(
        state,
        "blueBlockers",
        R.string.settings_blue_blocking_glasses,
        listOf(
            "no" to R.string.settings_no,
            "unrated" to R.string.settings_yes_rating_unknown,
            "rated" to R.string.settings_yes_i_know_the_rating,
        ),
        positions,
        onEvent,
    )
    if (state.answers["blueBlockers"].stringValue() == "rated") {
        val focus = LocalFocusManager.current
        CfTextField(
            rating,
            stringResource(R.string.settings_rating_mdfd),
            testTag = "settings.bodyClock.blueBlockerMdfd",
            hint = stringResource(R.string.settings_melanopic_daylight_filtering_den_d431eb),
            error = state.errors["blueBlockerMdfd"]?.let { settingsText(it) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            onKeyboardAction = { focus.clearFocus() },
        )
    }
}

@Composable
private fun ColumnScope.BodyClockHealth(
    state: BodyClockSectionState,
    positions: BodyClockQuestionPositions,
    onEvent: (BodyClockEvent) -> Unit,
) {
    BodyChoiceQuestion(
        state,
        "shiftWork",
        R.string.settings_in_the_past_14_days_have_you_wor_f04fc5,
        yesNo(),
        positions,
        onEvent,
        boolean = true,
    )
    SettingsPreferenceSwitch(
        stringResource(R.string.settings_add_movement_breaks_during_long_a0451c),
        stringResource(R.string.settings_your_plan_breaks_up_long_sleep_o_32b835),
        state.answers["flightMovementBreaks"] == kotlinx.serialization.json.JsonPrimitive(true),
        { onEvent(BodyClockEvent.Preference(state.group, "flightMovementBreaks", it)) },
        "settings.bodyClock.flightMovementBreaks",
    )
    BodyCaption(R.string.settings_chronoflow_is_intended_for_adult_a300cd)
    BodyCaption(
        R.string.settings_chronoflow_is_not_suitable_if_yo_4f49dd,
        Modifier.testTag("settings.bodyClock.audienceNotice"),
    )
}

private fun yesNo() = listOf("yes" to R.string.settings_yes, "no" to R.string.settings_no)

@Composable
private fun BodyChoiceQuestion(
    state: BodyClockSectionState,
    key: String,
    title: Int,
    options: List<Pair<String, Int>>,
    positions: BodyClockQuestionPositions,
    onEvent: (BodyClockEvent) -> Unit,
    boolean: Boolean = false,
    hint: Int? = null,
) {
    val tag = "settings.bodyClock.$key"
    SettingsQuestion(
        stringResource(title),
        key in state.missing,
        tag,
        Modifier.onPlaced { positions.questionPlaced(key, it) },
        hint = hint?.let { stringResource(it) },
        error = state.errors[key]?.let { settingsText(it) },
    ) {
        CfPillGroup(
            options.map { CfPillOption(it.first, stringResource(it.second)) },
            if (boolean) {
                BodyClockChoice.yesNoSelection(state.answers[key])
            } else {
                state.answers[key].stringValue().orEmpty()
            },
            { onEvent(BodyClockEvent.Choice(state.group, key, it)) },
            tag,
        )
    }
}

@Composable
private fun BodyTimePair(
    state: BodyClockSectionState,
    first: String,
    firstLabel: Int,
    second: String,
    secondLabel: Int,
    positions: BodyClockQuestionPositions,
    onEvent: (BodyClockEvent) -> Unit,
) {
    SettingsTimeTilePair(
        first = { BodyTimeTile(state, first, firstLabel, positions, onEvent) },
        second = { BodyTimeTile(state, second, secondLabel, positions, onEvent) },
    )
}

@Composable
private fun BodyTimeTile(
    state: BodyClockSectionState,
    field: String,
    label: Int,
    positions: BodyClockQuestionPositions,
    onEvent: (BodyClockEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(PlanUITokens.space1.dp)) {
        SettingsTimeTile(
            stringResource(label),
            state.answers[field].stringValue(),
            { onEvent(BodyClockEvent.OpenTime(field)) },
            "settings.bodyClock.$field",
            modifier = Modifier.onPlaced { positions.questionPlaced(field, it) },
        )
        state.errors[field]?.let {
            SettingsError(settingsText(it), Modifier.testTag("settings.bodyClock.$field.error"))
        }
    }
}

@Composable
private fun BodyCaption(
    text: Int,
    modifier: Modifier = Modifier,
) {
    Text(stringResource(text), modifier, style = CfTheme.type.screenBody, color = CfTheme.colors.mutedForeground)
}
