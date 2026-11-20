package net.chronoflow.feature.wizard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.feature.wizard.shell.WizardMetrics

enum class WizardBannerTone { Error, Info }

@Composable
fun WizardBanner(
    body: String,
    testTag: String,
    modifier: Modifier = Modifier,
    tone: WizardBannerTone = WizardBannerTone.Error,
    title: String? = null,
) {
    val colors = CfTheme.colors
    val shape = RoundedCornerShape(PlanUITokens.radiusXl.dp)
    Row(
        modifier
            .fillMaxWidth()
            .testTag(
                testTag,
            ).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
            .background(
                if (tone ==
                    WizardBannerTone.Error
                ) {
                    colors.authErrorBackground
                } else {
                    colors.wizardNoticeBackground
                },
                shape,
            ).border(
                AuthTokens.borderWidth.dp,
                if (tone ==
                    WizardBannerTone.Error
                ) {
                    colors.authErrorBorder
                } else {
                    colors.wizardNoticeBorder
                },
                shape,
            ).padding(horizontal = PlanUITokens.wizardContentGap.dp, vertical = PlanUITokens.space3.dp),
        horizontalArrangement = Arrangement.spacedBy(PlanUITokens.wizardNoticeGap.dp),
    ) {
        Icon(
            LucideGlyphs.Info.imageVector(),
            null,
            Modifier.padding(top = WizardMetrics.bannerGlyphTop.dp).size(PlanUITokens.wizardNoticeIconSize.dp),
            tint = colors.cardForeground,
        )
        Column(Modifier.weight(1f)) {
            title?.let { Text(it, color = colors.cardForeground, style = CfTheme.type.style(CfTypeScale.authStrong)) }
            Text(body, color = colors.cardForeground, style = CfTheme.type.style(CfTypeScale.screenBody))
        }
    }
}
