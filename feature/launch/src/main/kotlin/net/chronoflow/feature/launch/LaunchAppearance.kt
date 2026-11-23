package net.chronoflow.feature.launch

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import net.chronoflow.core.designsystem.generated.CfDarkColors
import net.chronoflow.core.designsystem.generated.CfLightColors

class LaunchAppearance(
    val night: Boolean,
) {
    val colors get() = if (night) CfDarkColors else CfLightColors

    fun resources(context: Context): Resources {
        val config = context.resources.configuration
        if ((config.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) ==
            night
        ) {
            return context.resources
        }
        val captured = Configuration(config)
        captured.uiMode = (captured.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
            (if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO)
        return context.createConfigurationContext(captured).resources
    }
}
