package net.chronoflow.core.designsystem.component

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicSecureTextField
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.theme.CfTheme

/** Caller owns state. Secret states use remember { TextFieldState() } or a ViewModel, never saveable state or a log. */
@Composable
fun CfTextField(
    state: TextFieldState,
    label: String,
    modifier: Modifier = Modifier,
    testTag: String? = null,
    placeholder: String? = null,
    hint: String? = null,
    error: String? = null,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onKeyboardAction: KeyboardActionHandler? = null,
    contentType: ContentType? = null,
    inputTransformation: InputTransformation? = null,
) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    FieldLayout(label, hint, error, testTag, modifier) {
        BasicTextField(
            state,
            Modifier.fillMaxWidth().fieldSemantics(label, error, contentType, testTag),
            enabled = enabled,
            inputTransformation = inputTransformation,
            textStyle = CfTheme.type.authBody.copy(color = CfTheme.colors.cardForeground),
            keyboardOptions = keyboardOptions,
            onKeyboardAction = onKeyboardAction,
            lineLimits = TextFieldLineLimits.SingleLine,
            interactionSource = source,
            cursorBrush = SolidColor(CfTheme.colors.ring),
            decorator =
                cfFieldDecorator(
                    focused,
                    error != null,
                    enabled,
                ) { FieldPlaceholder(state, placeholder, testTag) },
        )
    }
}

/** Caller owns state. Secret states use remember { TextFieldState() } or a ViewModel, never saveable state or a log. */
@Composable
fun CfSecureTextField(
    state: TextFieldState,
    label: String,
    modifier: Modifier = Modifier,
    testTag: String? = null,
    revealed: Boolean = false,
    hint: String? = null,
    error: String? = null,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    onKeyboardAction: KeyboardActionHandler? = null,
    contentType: ContentType? = ContentType.Password,
    inputTransformation: InputTransformation? = null,
) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    FieldLayout(label, hint, error, testTag, modifier) {
        BasicSecureTextField(
            state,
            Modifier.fillMaxWidth().fieldSemantics(label, error, contentType, testTag),
            enabled = enabled,
            inputTransformation = inputTransformation,
            textStyle = CfTheme.type.authBody.copy(color = CfTheme.colors.cardForeground),
            keyboardOptions = keyboardOptions,
            onKeyboardAction = onKeyboardAction,
            interactionSource = source,
            cursorBrush = SolidColor(CfTheme.colors.ring),
            textObfuscationMode = if (revealed) TextObfuscationMode.Visible else TextObfuscationMode.RevealLastTyped,
            decorator = cfFieldDecorator(focused, error != null, enabled),
        )
    }
}

private fun Modifier.fieldSemantics(
    label: String,
    problem: String?,
    type: ContentType?,
    tag: String?,
): Modifier =
    then(if (tag == null) Modifier else Modifier.testTag(tag)).semantics {
        contentDescription = label
        if (problem != null) error(problem)
        if (type != null) contentType = type
    }

@Composable
private fun FieldLayout(
    label: String,
    hint: String?,
    error: String?,
    tag: String?,
    modifier: Modifier = Modifier,
    field: @Composable () -> Unit,
) {
    val c = CfTheme.colors
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AuthTokens.fieldGap.dp)) {
        Text(label, Modifier.clearAndSetSemantics { }, color = c.cardForeground, style = CfTheme.type.authLabel)
        field()
        if (error != null) {
            Row(
                Modifier.clearAndSetSemantics { },
                horizontalArrangement = Arrangement.spacedBy(AuthTokens.brandGap.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    LucideGlyphs.CircleAlert.imageVector(),
                    null,
                    Modifier.size(AuthTokens.icon.dp),
                    tint = c.destructive,
                )
                Text(error, color = c.destructive, style = CfTheme.type.authHint)
            }
        } else if (hint != null) {
            Text(
                hint,
                if (tag == null) Modifier else Modifier.testTag("$tag.hint"),
                color = c.mutedForeground,
                style = CfTheme.type.authHint,
            )
        }
    }
}

@Composable
private fun FieldPlaceholder(
    state: TextFieldState,
    placeholder: String?,
    tag: String?,
) {
    if (placeholder != null && state.text.isEmpty()) {
        Text(
            placeholder,
            if (tag == null) Modifier else Modifier.testTag("$tag.placeholder"),
            color = CfTheme.colors.mutedForeground,
            style = CfTheme.type.authBody,
        )
    }
}

@Composable
internal fun cfFieldDecorator(
    focused: Boolean,
    error: Boolean,
    enabled: Boolean,
    placeholder: @Composable () -> Unit = {},
): TextFieldDecorator {
    val c = CfTheme.colors
    val shape = CfTheme.shapes.md
    return TextFieldDecorator { inner ->
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = PlanUITokens.actionMinHeight.dp)
                .alpha(if (enabled) 1f else AuthTokens.disabledOpacity)
                .border(
                    AuthTokens.borderWidth.dp,
                    if (error) {
                        c.destructive
                    } else if (focused) {
                        c.ring
                    } else {
                        c.input
                    },
                    shape,
                ).drawWithCache {
                    val width = AuthTokens.focusWidth.dp.toPx()
                    val outline =
                        shape.createOutline(
                            Size(size.width + width, size.height + width),
                            layoutDirection,
                            this,
                        )
                    val stroke = Stroke(width)
                    onDrawWithContent {
                        drawContent()
                        if (focused) translate(-width / 2, -width / 2) { drawOutline(outline, c.ring, style = stroke) }
                    }
                }.padding(horizontal = AuthTokens.inset.dp, vertical = AuthTokens.fieldGap.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            placeholder()
            inner()
        }
    }
}
