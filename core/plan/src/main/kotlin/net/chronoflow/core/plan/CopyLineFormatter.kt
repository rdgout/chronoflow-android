package net.chronoflow.core.plan

import net.chronoflow.core.model.contracts.AirportRef
import net.chronoflow.core.model.contracts.CaffeineAmountView
import net.chronoflow.core.model.contracts.CopyArg
import net.chronoflow.core.model.contracts.CopyArgKind
import net.chronoflow.core.model.contracts.CopyArgValue
import net.chronoflow.core.model.contracts.CopyArgument
import net.chronoflow.core.model.contracts.CopyLine
import net.chronoflow.core.model.contracts.Drink
import net.chronoflow.core.model.contracts.DrinkAmountView
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.core.model.contracts.TripDetailView
import kotlin.math.abs
import kotlin.math.floor

data class CopyFormat(
    val language: Language,
    val timeFormat: TimeFormat = TimeFormat.Value24,
    val displayZone: String? = null,
    val annotateInstantZone: Boolean = false,
    val trip: TripDetailView? = null,
)

class CopyLineFormatter(
    val catalog: CopyCatalog,
) {
    fun format(
        line: CopyLine,
        options: CopyFormat,
    ): String {
        val arguments = HashMap<String, String>()
        var count = 1.0
        for (argument in line.args) {
            when (argument) {
                is CopyArgument.Standard -> {
                    val arg = argument.value
                    arguments[arg.name] = formatArgument(arg, options)
                    if (arg.kind == CopyArgKind.Count && arg.name == "count") count = arg.value.number
                }

                is CopyArgument.Caffeine -> {
                    val arg = argument.value
                    arguments[arg.name] =
                        arg.usualDrinks?.let { usualDrinks(it, options.language) }
                            ?: caffeineAmount(CaffeineAmountView(mg = arg.value, drinks = arg.drinks), options.language)
                }
            }
        }
        return catalog.text(line.key, options.language, arguments, count)
    }

    fun formatArgument(
        arg: CopyArg,
        options: CopyFormat,
    ): String {
        val value = arg.value.text
        val number = arg.value.number
        return when (arg.kind) {
            CopyArgKind.Date -> {
                formatDate(value, options.language)
            }

            CopyArgKind.Instant -> {
                val zone = arg.tz ?: options.displayZone ?: "UTC"
                val time = formatInstant(value, options, zone)
                if (options.annotateInstantZone && zone != options.displayZone) {
                    "$time (${zoneLabel(zone, options.trip)})"
                } else {
                    time
                }
            }

            CopyArgKind.Offset -> {
                formatOffset(number, options.language)
            }

            CopyArgKind.Minutes -> {
                formatMinutes(number, options.language)
            }

            CopyArgKind.Count -> {
                formatNumber(number, options.language)
            }

            CopyArgKind.Milligrams -> {
                text(":amount mg", options.language, mapOf("amount" to formatNumber(number, options.language)))
            }

            else -> {
                value
            }
        }
    }

    fun formatNumber(
        value: Double,
        language: Language,
    ): String = formatPlanNumber(value, language)

    fun formatDate(
        value: String,
        language: Language,
        zone: String = "UTC",
    ): String = dateText(planDate(value), language, planZoneRules.rules(zone), DateTemplate.D_MMM_YYYY)

    fun formatDayMonth(
        value: String,
        language: Language,
        zone: String = "UTC",
    ): String = dateText(planDate(value), language, planZoneRules.rules(zone), DateTemplate.D_MMM)

    fun formatWeekdayDate(
        value: String,
        language: Language,
        zone: String = "UTC",
        month: Boolean = true,
    ): String =
        dateText(
            planDate(value),
            language,
            planZoneRules.rules(zone),
            if (month) DateTemplate.EEE_D_MMM else DateTemplate.EEE_D,
        )

    fun formatWeekday(
        value: String,
        language: Language,
    ): String = dateText(planDate(value), language, planZoneRules.rules("UTC"), DateTemplate.EEE)

    fun formatMonthYear(
        value: String,
        language: Language,
        zone: String = "UTC",
    ): String = dateText(planDate(value), language, planZoneRules.rules(zone), DateTemplate.MMM_YYYY)

    fun formatInstant(
        value: String,
        options: CopyFormat,
        zone: String? = null,
    ): String {
        val instant = planDate(value) ?: return ""
        val fields = localFields(instant, planZoneRules.rules(zone ?: options.displayZone ?: "UTC"))
        val out = StringBuilder(8)
        if (options.timeFormat != TimeFormat.Value12) {
            return out
                .append2(fields.hour)
                .append(':')
                .append2(fields.minute)
                .toString()
        }
        val hour = if (fields.hour % 12 == 0) 12 else fields.hour % 12
        out
            .append2(hour)
            .append(':')
            .append2(fields.minute)
            .append(' ')
        return out.append(text(if (fields.hour < 12) "AM" else "PM", options.language)).toString()
    }

    fun formatMinutes(
        value: Double,
        language: Language,
        duration: Boolean = false,
    ): String {
        val hours = floor(value / 60)
        val minutes = value % 60
        if (!duration) return text(":amount min", language, mapOf("amount" to formatNumber(value, language)))
        if (hours > 0 && minutes > 0) {
            return text(
                ":hours hr :minutes min",
                language,
                mapOf("hours" to formatNumber(hours, language), "minutes" to formatNumber(minutes, language)),
            )
        }
        return text(
            if (hours > 0) ":amount hr" else ":amount min",
            language,
            mapOf("amount" to formatNumber(if (hours > 0) hours else minutes, language)),
        )
    }

    fun formatOffset(
        value: Double,
        language: Language,
    ): String {
        val absolute = abs(value)
        val hours = floor(absolute / 60)
        val minutes = absolute % 60
        val duration =
            if (minutes != 0.0) {
                text(
                    ":hours h :minutes min",
                    language,
                    mapOf("hours" to formatNumber(hours, language), "minutes" to formatNumber(minutes, language)),
                )
            } else {
                text(":amount h", language, mapOf("amount" to formatNumber(hours, language)))
            }
        return (
            if (value > 0) {
                "+"
            } else if (value < 0) {
                "−"
            } else {
                ""
            }
        ) + duration
    }

    fun drink(
        amount: DrinkAmountView,
        language: Language,
        chip: Boolean = false,
    ): String {
        val key =
            when (amount.drink) {
                Drink.FilterCoffee, Drink.Espresso -> {
                    if (chip) ":amount coffee|:amount coffees" else "half a cup of coffee|:amount cups of coffee"
                }

                Drink.Tea -> {
                    if (chip) ":amount tea|:amount teas" else "half a cup of tea|:amount cups of tea"
                }

                Drink.Cola -> {
                    if (chip) {
                        ":amount soft drink|:amount soft drinks"
                    } else {
                        "half a can of soft drink|:amount cans of soft drink"
                    }
                }

                Drink.EnergyDrink -> {
                    if (chip) {
                        ":amount energy drink|:amount energy drinks"
                    } else {
                        "half a can of energy drink|:amount cans of energy drink"
                    }
                }

                else -> {
                    return ""
                }
            }
        val whole = if (amount.halves >= 2) formatNumber((amount.halves / 2).toDouble(), language) else ""
        val quantity = whole + if (amount.halves % 2 == 1) "½" else ""
        return catalog.text(key, language, mapOf("amount" to quantity), amount.halves.toDouble())
    }

    fun usualDrinks(
        count: Int,
        language: Language,
    ): String =
        catalog.text(
            ":count drink|:count drinks",
            language,
            mapOf("count" to formatNumber(count.toDouble(), language)),
            count.toDouble(),
        )

    fun caffeineAmount(
        amount: CaffeineAmountView,
        language: Language,
    ): String {
        val mg = text(":n mg", language, mapOf("n" to formatNumber(amount.mg.toDouble(), language)))
        val drinks = amount.drinks.map { drink(it, language) }
        val first = drinks.firstOrNull() ?: return mg
        val description =
            if (drinks.size == 2) {
                text(":first or :second", language, mapOf("first" to first, "second" to drinks[1]))
            } else {
                first
            }
        return text(":drinks (:mg)", language, mapOf("drinks" to description, "mg" to mg))
    }

    internal fun text(
        key: String,
        language: Language,
        arguments: Map<String, String> = emptyMap(),
    ): String = catalog.text(key, language, arguments)
}

val CopyArgValue.text: String
    get() =
        when (this) {
            is CopyArgValue.Text -> value
            is CopyArgValue.Numeric -> if (value.signum() == 0) "0" else value.stripTrailingZeros().toPlainString()
        }

val CopyArgValue.number: Double
    get() = text.toDoubleOrNull() ?: 0.0

fun zoneLabel(
    zone: String,
    trip: TripDetailView? = null,
): String {
    val airports: List<AirportRef> =
        trip?.let { listOf(it.from, it.to) + it.legs.flatMap { leg -> listOf(leg.from, leg.to) } }.orEmpty()
    return airports.firstOrNull { it.zone == zone && it.city != null }?.city
        ?: (zone.split('/').lastOrNull { it.isNotEmpty() } ?: zone).replace('_', ' ')
}
