package net.chronoflow.feature.settings.security

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.CfLightColors
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.designsystem.generated.SettingsTokens
import net.chronoflow.core.designsystem.theme.CfTheme
import kotlin.math.floor

@Composable
internal fun SettingsAuthenticatorQr(matrix: QrMatrix?) {
    val description = securityText("QR code for your authenticator app")
    val shape = RoundedCornerShape(PlanUITokens.radiusLg.dp)
    val ink = CfLightColors.foreground
    Box(
        Modifier
            .size(SettingsTokens.qrSize.dp)
            .clip(shape)
            .background(CfTheme.colors.settingsQrBackground)
            .border(AuthTokens.borderWidth.dp, CfTheme.colors.border, shape)
            .testTag("settings.twoFactor.qr")
            .semantics {
                contentDescription = description
                role = Role.Image
            }.padding(PlanUITokens.space2.dp)
            .drawWithCache {
                val path = Path()
                if (matrix != null) {
                    val module = floor(size.minDimension / matrix.size).coerceAtLeast(1f)
                    val left = floor((size.width - module * matrix.size) / 2)
                    val top = floor((size.height - module * matrix.size) / 2)
                    for (y in 0 until matrix.size) {
                        for (x in 0 until matrix.size) {
                            if (matrix[x, y]) {
                                path.addRect(
                                    Rect(
                                        left + x * module,
                                        top + y * module,
                                        left + (x + 1) * module,
                                        top + (y + 1) * module,
                                    ),
                                )
                            }
                        }
                    }
                }
                onDrawBehind { drawPath(path, ink) }
            },
    )
}
