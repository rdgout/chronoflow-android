package net.chronoflow.core.ui.plan

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import net.chronoflow.core.ui.l10n.generated.CopyIndex
import net.chronoflow.core.ui.l10n.generated.CopyTable

@Composable
fun planScreenText(key: String): String = stringResource(checkNotNull(CopyIndex.find(CopyTable.Screens, key)).resId)
