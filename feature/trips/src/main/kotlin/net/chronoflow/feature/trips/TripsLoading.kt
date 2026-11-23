package net.chronoflow.feature.trips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.component.CfSkeleton
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.ui.R

@Composable
internal fun TripsLoading(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.screens_loading_your_trips)
    val shape = RoundedCornerShape(PlanUITokens.radius3Xl.dp)
    Column(
        modifier.testTag("trips.loading").semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(PlanUITokens.space4.dp),
    ) {
        CfSkeleton(
            Modifier.fillMaxWidth(PlanUITokens.loadingHeadingWidthRatio).height(PlanUITokens.loadingHeadingHeight.dp),
            CircleShape,
        )
        CfSkeleton(Modifier.fillMaxWidth().height(PlanUITokens.tripsLoadingFirstHeight.dp), shape)
        CfSkeleton(
            Modifier.fillMaxWidth(PlanUITokens.loadingHeadingWidthRatio).height(PlanUITokens.loadingHeadingHeight.dp),
            CircleShape,
        )
        CfSkeleton(Modifier.fillMaxWidth().height(PlanUITokens.tripsLoadingSecondHeight.dp), shape)
    }
}
