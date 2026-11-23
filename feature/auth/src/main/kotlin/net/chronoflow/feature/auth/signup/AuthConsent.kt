package net.chronoflow.feature.auth.signup

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.designsystem.generated.CfTypeScale
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.ui.R
import net.chronoflow.feature.auth.model.AuthConsentText

@Composable
fun AuthConsent(
    policyOpener: PolicyOpener,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val text = stringResource(R.string.auth_by_creating_an_account_you_agree_to_9e09cb)
    val foreground = CfTheme.colors.foreground
    val annotated =
        remember(text, foreground, policyOpener, enabled) {
            buildAnnotatedString {
                AuthConsentText.parse(text).forEach { segment ->
                    when (segment) {
                        is AuthConsentText.Segment.Plain -> {
                            append(segment.text)
                        }

                        is AuthConsentText.Segment.Link -> {
                            withLink(
                                LinkAnnotation.Url(
                                    segment.policy.url,
                                    styles =
                                        TextLinkStyles(
                                            SpanStyle(color = foreground, textDecoration = TextDecoration.Underline),
                                        ),
                                    linkInteractionListener = {
                                        if (enabled) {
                                            policyOpener.open(
                                                segment.policy.url,
                                            )
                                        }
                                    },
                                ),
                            ) { append(segment.text) }
                        }
                    }
                }
            }
        }
    Text(
        annotated,
        modifier
            .fillMaxWidth()
            .testTag("sign-up.consent")
            .alpha(if (enabled) 1f else AuthTokens.disabledOpacity)
            .semantics { if (!enabled) disabled() },
        color = CfTheme.colors.mutedForeground,
        style = CfTheme.type.style(CfTypeScale.authMeta),
        textAlign = TextAlign.Center,
    )
}
