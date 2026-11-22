@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.chronoflow.core.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.generated.SettingsTokens
import net.chronoflow.core.designsystem.layout.BoardMetrics
import net.chronoflow.core.designsystem.layout.LocalSafeInsets
import net.chronoflow.core.designsystem.layout.WindowPolicy
import net.chronoflow.core.designsystem.system.CfBackButton
import net.chronoflow.core.designsystem.system.CfLargeTopAppBar
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.ui.R

@BoardMetrics("Set2FAWide", "Set2FAManageWide")
object SettingsWideMetrics {
    const val COLUMN = 560f
}

@Composable
fun SettingsLazyPage(
    title: Int,
    onBack: (() -> Unit)?,
    testTag: String,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val wide = currentWindowAdaptiveInfoV2().windowSizeClass.isWidthAtLeastBreakpoint(WindowPolicy.wideBreakpointDp)
    Scaffold(
        modifier.fillMaxSize().testTag(testTag).nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CfTheme.colors.background,
        contentColor = CfTheme.colors.cardForeground,
        contentWindowInsets = LocalSafeInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
        topBar = {
            CfLargeTopAppBar(
                stringResource(title),
                scrollBehavior,
                navigationIcon = onBack?.let { { CfBackButton(stringResource(R.string.auth_back), it) } },
            )
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .padding(horizontal = PlanUITokens.space4.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier =
                    Modifier
                        .widthIn(
                            max = if (wide) SettingsWideMetrics.COLUMN.dp else androidx.compose.ui.unit.Dp.Infinity,
                        ).fillMaxWidth()
                        .testTag("$testTag.content"),
                state = listState,
                contentPadding = PaddingValues(top = PlanUITokens.space1.dp, bottom = SettingsTokens.pageBottom.dp),
                verticalArrangement = Arrangement.spacedBy(PlanUITokens.space6.dp),
                content = content,
            )
        }
    }
}

@Composable
fun SettingsPage(
    title: Int,
    onBack: (() -> Unit)?,
    testTag: String,
    modifier: Modifier = Modifier,
    spacing: Dp = PlanUITokens.space6.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val wide = currentWindowAdaptiveInfoV2().windowSizeClass.isWidthAtLeastBreakpoint(WindowPolicy.wideBreakpointDp)
    Scaffold(
        modifier.fillMaxSize().testTag(testTag).nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CfTheme.colors.background,
        contentColor = CfTheme.colors.cardForeground,
        contentWindowInsets = LocalSafeInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
        topBar = {
            CfLargeTopAppBar(
                stringResource(title),
                scrollBehavior,
                navigationIcon = onBack?.let { { CfBackButton(stringResource(R.string.auth_back), it) } },
            )
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PlanUITokens.space4.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                Modifier
                    .widthIn(
                        max = if (wide) SettingsWideMetrics.COLUMN.dp else androidx.compose.ui.unit.Dp.Infinity,
                    ).fillMaxWidth()
                    .testTag("$testTag.content")
                    .padding(top = PlanUITokens.space1.dp, bottom = SettingsTokens.pageBottom.dp),
                verticalArrangement = Arrangement.spacedBy(spacing),
                content = content,
            )
        }
    }
}
