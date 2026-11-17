package net.chronoflow.core.plan

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.endsWith
import assertk.assertions.isEqualTo
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.CaffeineAmountView
import net.chronoflow.core.model.contracts.CaffeineCopyArg
import net.chronoflow.core.model.contracts.CopyArg
import net.chronoflow.core.model.contracts.CopyArgKind
import net.chronoflow.core.model.contracts.CopyArgValue
import net.chronoflow.core.model.contracts.CopyArgument
import net.chronoflow.core.model.contracts.CopyLine
import net.chronoflow.core.model.contracts.Drink
import net.chronoflow.core.model.contracts.DrinkAmountView
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.core.testing.Fixtures
import org.junit.Test
import java.math.BigDecimal

class CopyTests {
    @Serializable
    data class Golden(
        val id: String,
        val kind: String,
        val locale: String,
        val expected: String,
        val value: JsonPrimitive,
        val zone: String? = null,
        val timeFormat: String? = null,
        val style: String? = null,
    )

    private val f = CopyLineFormatter(testCatalog)

    @Test fun intlGoldens() {
        val cases =
            ApiJson.response.decodeFromString(
                ListSerializer(Golden.serializer()),
                Fixtures.text("intl-cases.json"),
            )
        assertThat(cases.size).isEqualTo(44)
        val mismatches = mutableListOf<String>()
        for (item in cases) {
            val language = Language.of(item.locale)
            val value =
                if (item.value.isString) {
                    CopyArgValue.Text(
                        item.value.content,
                    )
                } else {
                    CopyArgValue.Numeric(BigDecimal(item.value.content))
                }
            val options = CopyFormat(language, TimeFormat.of(item.timeFormat ?: "24"))
            val actual =
                when (item.kind) {
                    "instant" -> f.formatInstant(value.text, options, item.zone)
                    "date" -> f.formatDate(value.text, language, item.zone!!)
                    "minutes" -> f.formatMinutes(value.number, language, duration = item.style == "duration")
                    else -> f.formatOffset(value.number, language)
                }
            if (actual != item.expected) mismatches += "${item.id}: $actual"
        }
        assertThat(mismatches).isEqualTo(emptyList<String>())
    }

    @Test fun caffeineCases() {
        val values = listOf(1, 2, 3, 4, 5, 8)
        val en =
            listOf(
                "half a cup of coffee",
                "one cup of coffee",
                "1½ cups of coffee",
                "two cups of coffee",
                "2½ cups of coffee",
                "4 cups of coffee",
            )
        val nl =
            listOf(
                "een half kopje koffie",
                "één kopje koffie",
                "1½ kopje koffie",
                "twee kopjes koffie",
                "2½ kopjes koffie",
                "4 kopjes koffie",
            )
        val chips = listOf("½ coffee", "1 coffee", "1½ coffees", "2 coffees", "2½ coffees", "4 coffees")
        values.forEachIndexed { i, halves ->
            val amount = DrinkAmountView(Drink.FilterCoffee, halves)
            assertThat(f.drink(amount, Language.En)).isEqualTo(en[i])
            assertThat(f.drink(amount, Language.Nl)).isEqualTo(nl[i])
            assertThat(f.drink(amount, Language.En, chip = true)).isEqualTo(chips[i])
            assertThat(f.drink(amount, Language.Nl, chip = true)).endsWith(" koffie")
        }
        val bad = mutableListOf<String>()
        for (drink in listOf(Drink.Tea, Drink.Cola, Drink.EnergyDrink, Drink.Espresso)) {
            for (halves in values) {
                for (language in listOf(Language.En, Language.Nl)) {
                    val value = f.drink(DrinkAmountView(drink, halves), language)
                    if (value.isEmpty() || "|" in value || ":amount" in value ||
                        "{" in value
                    ) {
                        bad += "${drink.raw}/$halves/${language.raw}: $value"
                    }
                }
            }
        }
        assertThat(bad).isEqualTo(emptyList<String>())
        val amount =
            CaffeineAmountView(125, listOf(DrinkAmountView(Drink.FilterCoffee, 3), DrinkAmountView(Drink.Tea, 5)))
        assertThat(f.caffeineAmount(amount, Language.En)).isEqualTo("1½ cups of coffee or 2½ cups of tea (125 mg)")
        assertThat(f.caffeineAmount(amount, Language.Nl)).isEqualTo("1½ kopje koffie of 2½ kopjes thee (125 mg)")
        assertThat(f.caffeineAmount(CaffeineAmountView(125, emptyList()), Language.Nl)).isEqualTo("125 mg")
        assertThat(f.usualDrinks(1, Language.Nl)).isEqualTo("1 drankje")
        assertThat(f.usualDrinks(2, Language.En)).isEqualTo("2 drinks")
    }

    @Test fun argumentsPluralAndFallback() {
        for (language in listOf(Language.En, Language.Nl)) {
            val en = language == Language.En
            val options = CopyFormat(language)

            fun count(n: Int) =
                CopyLine(
                    ":count booked nights",
                    listOf(
                        CopyArgument.Standard(
                            CopyArg("count", CopyArgKind.Count, CopyArgValue.Numeric(BigDecimal(n)), null),
                        ),
                    ),
                )
            assertThat(f.format(count(2), options)).isEqualTo(if (en) "2 booked nights" else "2 geboekte nachten")
            assertThat(f.format(count(1), options)).isEqualTo(if (en) "1 booked night" else "1 geboekte nacht")
            val unknown =
                CopyLine(
                    "Unknown :place and :place2",
                    listOf(
                        CopyArgument.Standard(CopyArg("place", CopyArgKind.Place, CopyArgValue.Text("Tokyo"), null)),
                    ),
                )
            assertThat(f.format(unknown, options)).isEqualTo("Unknown Tokyo and :place2")
            val caffeine =
                CopyLine(
                    "Coffee nap: have about :mg, then nap.",
                    listOf(
                        CopyArgument.Caffeine(CaffeineCopyArg("mg", CopyArgKind.Milligrams, 125, null, emptyList(), 2)),
                    ),
                )
            assertThat(f.format(caffeine, options)).contains(if (en) "2 drinks" else "2 drankjes")
        }
        val catalog =
            CopyCatalog(
                """{"entries":[{"key":"Hello :name","arguments":["name"],"languages":{"en":{"kind":"single","formats":["Hello %1${'$'}@"]}}}]}"""
                    .encodeToByteArray(),
            )
        assertThat(catalog.text("Hello :name", Language.Nl, mapOf("name" to "Ada"))).isEqualTo("Hello Ada")
        val annotated = CopyFormat(Language.En, displayZone = "UTC", annotateInstantZone = true)
        assertThat(
            f.formatArgument(
                CopyArg("at", CopyArgKind.Instant, CopyArgValue.Text("2026-01-15T00:00Z"), "Asia/Tokyo"),
                annotated,
            ),
        ).isEqualTo("09:00 (Tokyo)")
        assertThat(f.formatArgument(CopyArg("text", CopyArgKind.Text, CopyArgValue.Text("verbatim"), null), annotated))
            .isEqualTo("verbatim")
        assertThat(
            f.formatArgument(
                CopyArg("n", CopyArgKind.Milligrams, CopyArgValue.Numeric(BigDecimal(125)), null),
                annotated,
            ),
        ).isEqualTo("125 mg")
    }

    @Test fun numericArgumentTextNeverUsesAnExponent() {
        val rows =
            mapOf(
                "2" to "2",
                "1.50" to "1.5",
                "1e3" to "1000",
                "0.000001" to "0.000001",
                "0" to "0",
                "0.00" to "0",
            )
        val actual = rows.keys.associateWith { CopyArgValue.Numeric(BigDecimal(it)).text }
        assertThat(actual).isEqualTo(rows)
    }

    @Test fun reviewFixturesResolve() {
        val failures = mutableListOf<String>()
        for (name in listOf("wizard/review", "wizard/needs-answers")) {
            unresolvedCopyLines(ApiJson.response.parseToJsonElement(Fixtures.text("$name.json")), name, failures)
        }
        assertThat(failures).isEqualTo(emptyList<String>())
    }
}
