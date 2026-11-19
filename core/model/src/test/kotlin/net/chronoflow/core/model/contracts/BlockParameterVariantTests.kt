package net.chronoflow.core.model.contracts

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import net.chronoflow.core.model.api.ApiJson
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class BlockParameterVariantTests(
    private val label: String,
    private val type: String,
    private val params: String,
) {
    companion object {
        private const val MG = """{"mg":80,"drinks":[]}"""

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun rows(): List<Array<String>> =
            listOf(
                row(
                    "flight",
                    """{"legIndex":0,"fromIata":"AMS","toIata":"JFK","overnight":false,"approximateTrack":true}""",
                ),
                row("layover", """{"kind":"connection","iata":"AMS"}"""),
                row("transfer", """{"direction":"arrival","mode":"passenger","modeAssumed":false}"""),
                row("movement_break", "{}"),
                row("zone_change", """{"arrivalIata":"JFK","departureIata":"AMS","offsetMin":-360}"""),
                row("sleep", """{"kind":"main","setting":"bed","opportunityMin":480}"""),
                row("nap", """{"kind":"standard","setting":"seat","lengthMin":20}"""),
                row("rest", """{"lengthMin":20}"""),
                row("avoid_light", """{"setting":"home","reduced":false}"""),
                row("seek_light", """{"source":"outdoor","coreMin":30}"""),
                row("light_ok", """{"untilUtc":null}"""),
                row("light_neutral", """{"untilUtc":null}"""),
                row(
                    "caffeine_serving",
                    """{"kind":"maintenance","amount":"one","chip":"one","combined":null,"drivingLine":false}""",
                ),
                row("caffeine_ok", """{"amount":"one","drivingLine":false}"""),
                row("caffeine_routine", """{"cups":2,"amountPerDrink":"one","singleByUtc":null,"lastByUtc":null}"""),
                row(
                    "no_caffeine",
                    """{"nextServingUtc":null,"exception":{"amount":"one","byUtc":"2026-10-13T12:00Z"}}""",
                ),
                row(
                    "caffeine_serving",
                    """{"kind":"maintenance","amount":$MG,"chip":{"drink":"tea","halves":2},"combined":null,"drivingLine":false}""",
                    "drinks",
                ),
                row("caffeine_ok", """{"amount":$MG,"drivingLine":false}""", "drinks"),
                row(
                    "no_caffeine",
                    """{"nextServingUtc":null,"exception":{"amount":$MG,"byUtc":"2026-10-13T12:00Z"}}""",
                    "drinks",
                ),
                row("note", """{"note":{"role":"info","clinicianLink":false,"lines":[],"endState":null}}"""),
            )

        private fun row(
            type: String,
            params: String,
            variant: String = "",
        ) = arrayOf(if (variant.isEmpty()) type else "$type $variant", type, params)
    }

    @Test
    fun blockParameterVariants() {
        val value = blockView(type, params = params)
        val encoded = ApiJson.response.encodeToString(BlockView.serializer(), value)
        assertThat(ApiJson.response.decodeFromString(BlockView.serializer(), encoded)).isEqualTo(value)
        when (label) {
            "caffeine_routine" -> {
                val routine = (value.params as BlockParams.CaffeineRoutine).value
                assertThat(routine.amountPerDrink).isEqualTo("one")
                assertThat(routine.cups).isEqualTo(2)
            }

            "caffeine_serving drinks" -> {
                assertThat(value.params).isInstanceOf<BlockParams.CaffeineServingDrinks>()
            }

            "caffeine_ok drinks" -> {
                assertThat(value.params).isInstanceOf<BlockParams.CaffeineOKDrinks>()
            }

            "no_caffeine drinks" -> {
                assertThat(value.params).isInstanceOf<BlockParams.NoCaffeineDrinks>()
            }

            "caffeine_serving" -> {
                assertThat(value.params).isInstanceOf<BlockParams.CaffeineServing>()
            }
        }
    }
}
