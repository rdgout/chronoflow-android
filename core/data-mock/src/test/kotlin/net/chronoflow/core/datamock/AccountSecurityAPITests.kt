package net.chronoflow.core.datamock

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.api.AccountSecurityChange
import net.chronoflow.core.model.api.AccountSecurityResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class AccountSecurityAPITests {
    @Test fun mockRunsSetupCodesManageAndPasswordAndRejectsOffline() =
        runTest(timeout = 60.seconds) {
            val source = mockSource()
            login(source)
            assertEquals(
                DataFailure.Validation(
                    mapOf("current_password" to FieldMessage("The provided password was incorrect.")),
                    422,
                ),
                failure { source.accountSecurity(AccountSecurityChange.EnableTwoFactor("wrong")) },
            )
            assertTrue(
                source.accountSecurity(
                    AccountSecurityChange.EnableTwoFactor("password"),
                ) is AccountSecurityResult.Setup,
            )
            val codes =
                (
                    source.accountSecurity(
                        AccountSecurityChange.ConfirmTwoFactor("123456"),
                    ) as AccountSecurityResult.Codes
                ).codes
            assertEquals(8, codes.recoveryCodes.size)
            assertEquals(JsonPrimitive(true), source.fetchAccount().security["twoFactorEnabled"])
            source.accountSecurity(AccountSecurityChange.RevealRecoveryCodes("password"))
            source.accountSecurity(AccountSecurityChange.RegenerateRecoveryCodes("password"))
            source.accountSecurity(AccountSecurityChange.DisableTwoFactor("password"))
            assertEquals(JsonPrimitive(false), source.fetchAccount().security["twoFactorEnabled"])
            assertEquals(JsonNull, source.fetchAccount().security["twoFactorConfirmedAt"])
            source.accountSecurity(AccountSecurityChange.Password("password", "New password1!", "New password1!"))
            source.setOffline(true)
            assertEquals(
                DataFailure.Offline,
                failure {
                    source.accountSecurity(AccountSecurityChange.EnableTwoFactor("New password1!"))
                },
            )
        }
}
