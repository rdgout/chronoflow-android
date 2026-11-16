package net.chronoflow.core.model.contracts

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.serialization.Serializable
import net.chronoflow.core.model.api.ApiJson
import org.junit.Test

class RawEnumSerializerTests {
    @Serializable(with = Fruit.Serializer::class)
    sealed interface Fruit : RawEnum {
        data object Apple : Fruit {
            override val raw: String = "apple"
        }

        data class Unknown(
            override val raw: String,
        ) : Fruit

        companion object {
            fun of(raw: String): Fruit =
                when (raw) {
                    "apple" -> Apple
                    else -> Unknown(raw)
                }
        }

        object Serializer : RawEnumSerializer<Fruit>("test.Fruit", { Fruit.of(it) })
    }

    private fun decode(json: String) = ApiJson.response.decodeFromString(Fruit.serializer(), json)

    @Test
    fun knownRawDecodesToItsObject() {
        assertThat(decode("\"apple\"")).isEqualTo(Fruit.Apple)
    }

    @Test
    fun unknownRawSurvives() {
        assertThat(decode("\"future\"")).isEqualTo(Fruit.Unknown("future"))
    }

    @Test
    fun bothRoundTrip() {
        listOf(Fruit.Apple, Fruit.Unknown("future")).forEach {
            val text = ApiJson.response.encodeToString(Fruit.serializer(), it)
            assertThat(decode(text)).isEqualTo(it)
        }
    }
}
