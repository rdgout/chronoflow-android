package net.chronoflow.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.generated.SettingsTokens

@Immutable
class CfShapes {
    val sm = RoundedCornerShape(PlanUITokens.radiusSm.dp)
    val md = RoundedCornerShape(PlanUITokens.radiusMd.dp)
    val lg = RoundedCornerShape(PlanUITokens.radiusLg.dp)
    val xl = RoundedCornerShape(PlanUITokens.radiusXl.dp)
    val x2l = RoundedCornerShape(PlanUITokens.radius2Xl.dp)
    val x3l = RoundedCornerShape(PlanUITokens.radius3Xl.dp)
    val full = RoundedCornerShape(50)
    val sheetTop = RoundedCornerShape(topStart = SettingsTokens.sheetRadius.dp, topEnd = SettingsTokens.sheetRadius.dp)
}

@Immutable
class CfSpacing {
    val s1 = PlanUITokens.space1.dp
    val s2 = PlanUITokens.space2.dp
    val s3 = PlanUITokens.space3.dp
    val s4 = PlanUITokens.space4.dp
    val s5 = PlanUITokens.space5.dp
    val s6 = PlanUITokens.space6.dp
    val s8 = PlanUITokens.space8.dp
    val s14 = PlanUITokens.space14.dp
    val s16 = PlanUITokens.space16.dp
}

object ReduceMotion {
    fun resolve(
        animatorsEnabled: Boolean,
        override: Boolean?,
    ): Boolean = override ?: !animatorsEnabled
}
