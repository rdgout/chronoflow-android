package net.chronoflow.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.effect.cfClickable
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.theme.CfTheme

@Composable
fun CfBanner(
    title: String,
    message: String?,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    announce: Boolean = true,
    testTag: String? = null,
    actionTestTag: String? = null,
) {
    val c = CfTheme.colors
    val shape = RoundedCornerShape(AuthTokens.bannerRadius.dp)
    val hasAction = actionLabel != null && onAction != null
    Row(
        modifier
            .fillMaxWidth()
            .then(if (testTag == null) Modifier else Modifier.testTag(testTag))
            .semantics(mergeDescendants = !hasAction) { if (announce) liveRegion = LiveRegionMode.Polite }
            .background(c.authErrorBackground, shape)
            .border(AuthTokens.borderWidth.dp, c.authErrorBorder, shape)
            .padding(horizontal = AuthTokens.bannerHorizontal.dp, vertical = AuthTokens.bannerVertical.dp),
        horizontalArrangement = Arrangement.spacedBy(AuthTokens.bannerGap.dp),
    ) {
        Icon(
            LucideGlyphs.CircleAlert.imageVector(),
            null,
            Modifier.padding(top = AuthTokens.bannerIconTop.dp).size(AuthTokens.bannerGlyph.dp),
            tint = c.destructive,
        )
        Column(Modifier.weight(1f)) {
            Text(title, color = c.cardForeground, style = CfTheme.type.authStrong)
            if (!message.isNullOrEmpty()) Text(message, color = c.cardForeground, style = CfTheme.type.authHint)
            if (hasAction) {
                Text(
                    requireNotNull(actionLabel),
                    Modifier
                        .minimumInteractiveComponentSize()
                        .heightIn(min = AuthTokens.touch.dp)
                        .then(if (actionTestTag == null) Modifier else Modifier.testTag(actionTestTag))
                        .cfClickable(requireNotNull(onAction))
                        .padding(vertical = AuthTokens.fieldGap.dp),
                    color = c.foreground,
                    style = CfTheme.type.authStrong,
                    textDecoration = TextDecoration.Underline,
                )
            }
        }
    }
}

enum class CfNoticeTone { Info, Warning }

@Composable
fun CfNotice(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    tone: CfNoticeTone = CfNoticeTone.Info,
) {
    val c = CfTheme.colors
    val warning = tone == CfNoticeTone.Warning
    val shape = RoundedCornerShape(AuthTokens.noticeRadius.dp)
    Row(
        modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { }
            .background(if (warning) c.settingsWarningBackground else c.card, shape)
            .border(AuthTokens.borderWidth.dp, if (warning) c.settingsWarningBorder else c.border, shape)
            .padding(AuthTokens.inset.dp),
        horizontalArrangement = Arrangement.spacedBy(AuthTokens.inset.dp),
    ) {
        Icon(
            (if (warning) LucideGlyphs.CircleAlert else LucideGlyphs.Info).imageVector(),
            null,
            Modifier.padding(top = AuthTokens.noticeIconTop.dp).size(AuthTokens.icon.dp),
            tint = if (warning) c.dawn else c.mutedForeground,
        )
        val strong = CfTheme.type.authStrong.toSpanStyle()
        val text =
            buildAnnotatedString {
                withStyle(strong) { append(title) }
                append(" ")
                append(message)
            }
        Text(text, Modifier.weight(1f), color = c.cardForeground, style = CfTheme.type.authHint)
    }
}

@Composable
fun CfSkeleton(
    modifier: Modifier = Modifier,
    shape: Shape = CfTheme.shapes.md,
) {
    Box(modifier.background(CfTheme.colors.muted, shape).clearAndSetSemantics { })
}
