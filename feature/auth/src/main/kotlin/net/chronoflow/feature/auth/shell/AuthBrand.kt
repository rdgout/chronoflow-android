package net.chronoflow.feature.auth.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.effect.cfClickable
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.ui.R

@Composable
internal fun AuthBrand(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    showsBack: Boolean = true,
    enabled: Boolean = true,
    announcementRevision: Int = 0,
    takeAnnouncement: () -> String? = { null },
) {
    Row(
        modifier.heightIn(min = AuthMetrics.TOUCH.dp).testTag("auth.brand.row"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showsBack) {
            val back = stringResource(R.string.auth_back)
            Box(
                Modifier
                    .size(
                        AuthMetrics.TOUCH.dp,
                    ).testTag("auth.back")
                    .alpha(if (enabled) 1f else AuthTokens.disabledOpacity)
                    .semantics {
                        contentDescription =
                            back
                    }.cfClickable(onBack, enabled = enabled, focusShape = CfTheme.shapes.full),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    LucideGlyphs.ChevronLeft.imageVector(),
                    null,
                    Modifier.size(AuthTokens.backGlyph.dp),
                    tint = CfTheme.colors.skyTitle,
                )
            }
        } else {
            Spacer(Modifier.size(AuthMetrics.TOUCH.dp))
        }
        Spacer(Modifier.weight(1f))
        Row(
            Modifier.testTag("auth.brand").semantics(mergeDescendants = true) {
            },
            horizontalArrangement =
                Arrangement.spacedBy(
                    AuthTokens.brandGap.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(LucideGlyphs.Moon.imageVector(), null, Modifier.size(AuthTokens.moon.dp), tint = CfTheme.colors.dawn)
            Text(
                stringResource(R.string.auth_chronoflow),
                color = CfTheme.colors.skyTitle,
                style = CfTheme.type.style(CfTypeScale.authBrand),
            )
        }
        Spacer(Modifier.weight(1f))
        AuthAnnouncer(announcementRevision, takeAnnouncement, Modifier.size(AuthMetrics.TOUCH.dp))
    }
}
