package net.chronoflow.core.model.contracts

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEmpty
import assertk.assertions.isSameInstanceAs
import assertk.assertions.isTrue
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.testing.Fixtures
import org.junit.Test
import java.math.BigDecimal

class PlanDecodingTests {
    private fun completePlan(name: String) {
        val value = ApiJson.response.decodeFromString(Parts.serializer(), Fixtures.text("plans/$name.json"))
        assertThat(value.plan.blocks.any { it.type == BlockType.Flight }).isTrue()
        assertThat(value.plan.days).isNotEmpty()
        val encoded = ApiJson.response.encodeToString(Parts.serializer(), value)
        assertThat(ApiJson.response.decodeFromString(Parts.serializer(), encoded)).isEqualTo(value)
    }

    @Test fun completePlansS1() = completePlan("S1")

    @Test fun completePlansS2() = completePlan("S2")

    @Test fun completePlansS3() = completePlan("S3")

    @Test fun completePlansS4() = completePlan("S4")

    @Test fun completePlansS5() = completePlan("S5")

    @Test fun completePlansS6() = completePlan("S6")

    @Test fun completePlansS6Return10N() = completePlan("S6-RETURN-10N")

    @Test
    fun fullEnvelopesAndMissingNullableFields() {
        val items =
            ApiJson.response
                .decodeFromString(
                    TripListResponse.serializer(),
                    Fixtures.text("trips/list.json"),
                ).trips
        val detail = TripDetailResponse.decode(Fixtures.bytes("trips/detail.json"))
        assertThat(items.first().card.id).isEqualTo(detail.detail.trip.id)
        assertThat(items.first().adaptation).isEqualTo(
            detail.detail.plan.summary
                ?.adaptation,
        )
        assertThat(items.first().planInputHash).isEqualTo(detail.detail.plan.meta.inputHash)
        val stripped = removingNulls(ApiJson.response.parseToJsonElement(detail.data.decodeToString()))
        val again =
            TripDetailResponse.decode(
                ApiJson.response.encodeToString(JsonElement.serializer(), stripped).encodeToByteArray(),
            )
        assertThat(again.detail).isEqualTo(detail.detail)
        assertFailure { TripDetailResponse.decode("{}".encodeToByteArray()) }
    }

    @Test
    fun copyArgumentShapesAndFutureEnums() {
        val text =
            """{"key":"Test","args":[{"name":"place","kind":"place","value":"Tokyo"},""" +
                """{"name":"min","kind":"future","value":1.5},""" +
                """{"name":"mg","kind":"milligrams","value":80,"drinks":[{"drink":"future","halves":2}],"usualDrinks":2}]}"""
        val line = ApiJson.response.decodeFromString(CopyLine.serializer(), text)
        assertThat(line.args.size).isEqualTo(3)
        assertThat(line.args[1])
            .isEqualTo(
                CopyArgument.Standard(
                    CopyArg("min", CopyArgKind.Unknown("future"), CopyArgValue.Numeric(BigDecimal("1.5")), null),
                ),
            )
        val caffeine = line.args[2] as CopyArgument.Caffeine
        assertThat(
            caffeine.value.drinks
                .first()
                .drink,
        ).isEqualTo(Drink.Unknown("future"))
        val encoded = ApiJson.response.encodeToString(CopyLine.serializer(), line)
        assertThat(ApiJson.response.decodeFromString(CopyLine.serializer(), encoded)).isEqualTo(line)
        assertThat(
            ApiJson.response.decodeFromString(CheckStatus.serializer(), "\"future\""),
        ).isEqualTo(CheckStatus.Unknown("future"))
    }

    @Test
    fun unknownLaneOnKnownBlockIsNotRenderable() {
        val value = blockView("rest", lane = "future_lane", params = """{"lengthMin":20}""")
        assertThat(value.type).isEqualTo(BlockType.Rest)
        assertThat(value.lane).isEqualTo(Lane.Unknown("future_lane"))
        assertThat(value.isSupported).isFalse()
        val encoded = ApiJson.response.encodeToString(BlockView.serializer(), value)
        assertThat(ApiJson.response.decodeFromString(BlockView.serializer(), encoded)).isEqualTo(value)
    }

    @Test
    fun rawBytesAreKeptWhenNothingIsRetired() {
        val bytes = Fixtures.bytes("trips/detail.json")
        assertThat(TripDetailResponse.decode(bytes).data).isSameInstanceAs(bytes)
        val root = fixtureObject("trips/detail.json")
        val futureText = blockText("future_step", "future_lane", """{"future":[1,null,"kept"]}""")
        val future = ApiJson.response.parseToJsonElement(futureText) as JsonObject
        val plan = root["plan"] as JsonObject
        val blocks = JsonArray((plan["blocks"] as JsonArray) + future)
        val patched = root.with("plan", plan.with("blocks", blocks))
        val response =
            TripDetailResponse.decode(
                ApiJson.response.encodeToString(JsonObject.serializer(), patched).encodeToByteArray(),
            )
        val last =
            response.detail.plan.blocks
                .last()
        assertThat(last.type).isEqualTo(BlockType.Unknown("future_step"))
        assertThat(last.lane).isEqualTo(Lane.Unknown("future_lane"))
        assertThat(
            response.detail.plan.renderableBlocks
                .any { it.id == last.id },
        ).isFalse()
        assertThat(last.params).isInstanceOf<BlockParams.Unknown>()
    }
}

internal fun blockText(
    type: String,
    lane: String = "caffeine",
    params: String,
): String =
    """{"id":"test-block","lane":"$lane","type":"$type","startUtc":"2026-10-13T10:00Z","endUtc":"2026-10-13T11:00Z","displayTz":"Europe/Amsterdam","priority":"optional","text":{"key":"Test","args":[]},"params":$params,"actionable":true,"checkKey":{"type":"$type","localDate":"2026-10-13","ordinal":1}}"""

internal fun blockView(
    type: String,
    lane: String = "caffeine",
    params: String,
): BlockView = ApiJson.response.decodeFromString(BlockView.serializer(), blockText(type, lane, params))
