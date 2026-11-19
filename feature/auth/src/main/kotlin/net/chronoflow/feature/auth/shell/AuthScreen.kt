package net.chronoflow.feature.auth.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.component.CfCard
import net.chronoflow.core.designsystem.effect.skyBackground
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.layout.LocalSafeInsets
import net.chronoflow.core.designsystem.theme.CfTheme

@Composable
fun AuthScreen(
    scrollTag: String,
    title: String,
    description: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    showsBack: Boolean = true,
    compact: Boolean = false,
    headingGlyph: AuthHeadingGlyph? = null,
    enabled: Boolean = true,
    announcementRevision: Int = 0,
    takeAnnouncement: () -> String? = { null },
    footer: (@Composable () -> Unit)? = null,
    card: @Composable ColumnScope.() -> Unit,
) {
    val coordinates = remember { AuthCoordinates() }
    Box(modifier.fillMaxSize().skyBackground(CfTheme.colors.isDark).semantics { paneTitle = title }) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val columnWidth =
                if (maxWidth >=
                    AuthMetrics.WIDE_BREAKPOINT.dp
                ) {
                    Modifier.widthIn(max = AuthMetrics.WIDE_COLUMN.dp).fillMaxWidth()
                } else {
                    Modifier.fillMaxWidth()
                }
            CompositionLocalProvider(LocalAuthCoordinates provides coordinates) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .imePadding()
                        .onPlaced { coordinates.viewport = it }
                        .verticalScroll(rememberScrollState())
                        .onPlaced { coordinates.content = it }
                        .bringIntoViewRequester(coordinates.requester)
                        .testTag(scrollTag)
                        .windowInsetsPadding(LocalSafeInsets.current)
                        .padding(
                            start = AuthTokens.gutter.dp,
                            top = AuthTokens.gutter.dp,
                            end = AuthTokens.gutter.dp,
                            bottom = AuthTokens.bottom.dp,
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AuthBrand(onBack, columnWidth, showsBack, enabled, announcementRevision, takeAnnouncement)
                    Column(
                        columnWidth.padding(top = if (compact) AuthTokens.spacer.dp else AuthTokens.headingMargin.dp),
                    ) {
                        if (headingGlyph != null) {
                            AuthHeadingBadge(headingGlyph)
                            Spacer(Modifier.height((2 * AuthTokens.fieldGap).dp))
                        }
                        Text(
                            title,
                            Modifier.testTag("auth.heading").semantics {
                                heading()
                            },
                            color = CfTheme.colors.skyTitle,
                            style = CfTheme.type.style(CfTypeScale.authHeading),
                        )
                        Spacer(Modifier.height(AuthTokens.fieldGap.dp))
                        Text(
                            description,
                            Modifier.testTag("auth.subtitle"),
                            color = CfTheme.colors.skyTitle,
                            style = CfTheme.type.authBody,
                        )
                    }
                    Spacer(Modifier.height(AuthTokens.spacer.dp))
                    Spacer(Modifier.weight(1f))
                    CfCard(
                        columnWidth.testTag("auth.card"),
                        border = false,
                        spacing = AuthTokens.cardGap.dp,
                        content = card,
                    )
                    if (footer != null) {
                        Spacer(Modifier.height(AuthTokens.footerGap.dp))
                        Box(columnWidth.testTag("auth.footer")) { footer() }
                    }
                }
            }
        }
    }
}
