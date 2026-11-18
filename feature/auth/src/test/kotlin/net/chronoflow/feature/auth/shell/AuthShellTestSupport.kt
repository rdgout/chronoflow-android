package net.chronoflow.feature.auth.shell

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.component.CfButton
import net.chronoflow.core.designsystem.component.CfTextField
import net.chronoflow.core.designsystem.generated.AuthTokens
import net.chronoflow.core.ui.R

internal fun ComposeContentTestRule.bounds(tag: String): DpRect =
    onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()

@Composable
internal fun AuthShellSample() {
    AuthScreen(
        "sample.scroll",
        stringResource(
            R.string.auth_welcome_back,
        ),
        stringResource(R.string.auth_sign_in_to_see_your_trips_and_plans),
        {
        },
        footer = {
            CfButton(stringResource(R.string.auth_new_here_plan_a_trip_first), {
            }, Modifier.testTag("sample.footer"), fillWidth = true, minHeight = AuthTokens.footerHeight.dp)
        },
    ) {
        CfTextField(remember { TextFieldState() }, stringResource(R.string.auth_email), testTag = "sample.email")
        CfTextField(remember { TextFieldState() }, stringResource(R.string.auth_password), testTag = "sample.password")
        CfButton(stringResource(R.string.auth_sign_in), {}, Modifier.testTag("sample.submit"), fillWidth = true)
    }
}

@Composable
internal fun AuthLinksSample() {
    AuthLinkRow(modifier = Modifier.fillMaxWidth(), forgot = {
        AuthLink(stringResource(R.string.auth_forgot_your_password), {}, Modifier.testTag("sign-in.forgot"))
    }, signUp = {
        AuthLink(stringResource(R.string.auth_create_an_account), {}, Modifier.testTag("sign-in.signup"), strong = true)
    })
}
