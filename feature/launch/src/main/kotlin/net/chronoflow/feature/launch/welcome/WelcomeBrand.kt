package net.chronoflow.feature.launch.welcome

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.generated.WelcomeTokens
import net.chronoflow.core.designsystem.icon.LucideGlyphs
import net.chronoflow.core.designsystem.icon.imageVector
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.ui.R
import net.chronoflow.feature.launch.model.MoonAnchorStore

@Composable
internal fun WelcomeBrand(
    moonAnchorStore: MoonAnchorStore?,
    modifier: Modifier = Modifier,
    wordmarkModifier: Modifier = Modifier,
) {
    val density = LocalDensity.current.density
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(WelcomeTokens.brandGap.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = rememberVectorPainter(LucideGlyphs.Moon.imageVector()),
            contentDescription = null,
            colorFilter = ColorFilter.tint(CfTheme.colors.dawn),
            modifier = Modifier.size(WelcomeTokens.moonSize.dp).reportMoonAnchor(moonAnchorStore, density),
        )
        Text(
            stringResource(R.string.welcome_chronoflow),
            wordmarkModifier.testTag(WelcomeTags.WORDMARK),
            color = CfTheme.colors.skyTitle,
            style = CfTheme.type.welcomeWordmark,
        )
    }
}
