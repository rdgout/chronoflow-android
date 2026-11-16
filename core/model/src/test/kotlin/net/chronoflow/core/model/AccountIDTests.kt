package net.chronoflow.core.model

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import net.chronoflow.core.model.api.ApiJson
import org.junit.Test

class AccountIDTests {
    private val uuid = "123e4567-e89b-12d3-a456-426614174000"

    @Test
    fun upperCaseInputIsLowerCased() {
        assertThat(AccountID.of(uuid.uppercase()).rawValue).isEqualTo(uuid)
    }

    @Test
    fun thirtyFiveCharactersThrow() {
        assertFailure { AccountID.of(uuid.dropLast(1)) }.isInstanceOf<AccountID.InvalidUUID>()
    }

    @Test
    fun thirtySevenCharactersThrow() {
        assertFailure { AccountID.of(uuid + "0") }.isInstanceOf<AccountID.InvalidUUID>()
    }

    @Test
    fun shortGroupsThrow() {
        assertFailure { AccountID.of("1-1-1-1-1") }.isInstanceOf<AccountID.InvalidUUID>()
    }

    @Test
    fun nonHexThrows() {
        assertFailure { AccountID.of("zzzzzzzz-e89b-12d3-a456-426614174000") }.isInstanceOf<AccountID.InvalidUUID>()
    }

    @Test
    fun jsonRoundTrips() {
        val id = AccountID.of(uuid)
        val text = ApiJson.response.encodeToString(AccountID.serializer(), id)
        assertThat(text).isEqualTo("\"$uuid\"")
        assertThat(ApiJson.response.decodeFromString(AccountID.serializer(), text)).isEqualTo(id)
    }
}
