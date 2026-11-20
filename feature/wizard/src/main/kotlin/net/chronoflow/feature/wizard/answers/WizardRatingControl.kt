package net.chronoflow.feature.wizard.answers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.drop
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.shell.WizardMetrics
import net.chronoflow.feature.wizard.sleep.clockString
import net.chronoflow.feature.wizard.ui.WizardCopy
import net.chronoflow.feature.wizard.ui.resolve

@Composable
internal fun WizardRatingControl(
    state: WizardAnswerSheetState,
    onEvent: (WizardEvent) -> Unit,
) {
    if (state.values["blue_blockers"] != JsonPrimitive("rated")) return
    val raw = state.values["blue_blocker_mdfd"].clockString().orEmpty()
    val field = remember(state.prompt.id) { TextFieldState(raw) }
    val latest = rememberUpdatedState(state)
    val events = rememberUpdatedState(onEvent)
    LaunchedEffect(raw) {
        if (field.text.toString() != raw) field.edit { replace(0, length, raw) }
    }
    LaunchedEffect(field) {
        snapshotFlow { field.text.toString() }.drop(1).collect { text ->
            if (text !=
                latest.value.values["blue_blocker_mdfd"]
                    .clockString()
                    .orEmpty()
            ) {
                events.value(WizardEvent.UpdateAnswer("blue_blocker_mdfd", JsonPrimitive(text)))
            }
        }
    }
    val c = CfTheme.colors
    val shape = RoundedCornerShape(PlanUITokens.wizardCardRadius.dp)
    Box(
        if (state.error ==
            null
        ) {
            Modifier
        } else {
            Modifier.testTag("wizard.answer.rating.ring").answerRing(c.destructive)
        },
    ) {
        Row(
            Modifier
                .background(c.background, shape)
                .then(
                    if (state.error ==
                        null
                    ) {
                        Modifier.border(AuthTokens.borderWidth.dp, c.border, shape)
                    } else {
                        Modifier
                    },
                ).heightIn(min = WizardMetrics.sheetRowMinHeight.dp)
                .padding(horizontal = PlanUITokens.space4.dp),
            horizontalArrangement = Arrangement.spacedBy(PlanUITokens.space3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                WizardCopy("Rating on the label").resolve(),
                Modifier.weight(1f),
                color = c.foreground,
                style = CfTheme.type.style(CfTypeScale.wizardRow),
            )
            BasicTextField(
                field,
                Modifier
                    .testTag(
                        "wizard.answer.rating",
                    ).width(PlanUITokens.actionMinHeight.dp)
                    .heightIn(min = PlanUITokens.actionMinHeight.dp),
                textStyle =
                    CfTheme.type.style(CfTypeScale.wizardRow).copy(
                        color = c.foreground,
                        textAlign = TextAlign.End,
                        fontFeatureSettings = "tnum",
                    ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                lineLimits = TextFieldLineLimits.SingleLine,
                cursorBrush = SolidColor(c.dawn),
                decorator = { innerField ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) { innerField() }
                },
            )
        }
    }
}
