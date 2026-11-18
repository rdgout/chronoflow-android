package net.chronoflow.feature.auth.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.feature.auth.model.AuthBannerKind
import net.chronoflow.feature.auth.model.AuthFormContext

@Composable
internal fun AuthStatesSample() {
    Column(
        Modifier
            .fillMaxSize()
            .background(
                CfTheme.colors.background,
            ).verticalScroll(rememberScrollState())
            .padding(AuthTokens.gutter.dp),
        verticalArrangement = Arrangement.spacedBy(AuthTokens.inset.dp),
    ) {
        AuthBanner(AuthBannerKind.Credentials)
        AuthBanner(AuthBannerKind.Locked)
        AuthBanner(AuthBannerKind.Server)
        AuthBanner(AuthBannerKind.EmailTaken, context = AuthFormContext.SignUp)
        AuthBanner(AuthBannerKind.Server, context = AuthFormContext.SignUp)
        AuthBanner(AuthBannerKind.Locked, context = AuthFormContext.Forgot)
        AuthBanner(AuthBannerKind.Server, context = AuthFormContext.Forgot)
        AuthBanner(AuthBannerKind.Server, context = AuthFormContext.Reset)
        AuthBanner(AuthBannerKind.Locked, context = AuthFormContext.TwoFactor)
        AuthBanner(AuthBannerKind.Server, context = AuthFormContext.TwoFactor)
        AuthBanner(AuthBannerKind.Server, context = AuthFormContext.Verify)
    }
}
