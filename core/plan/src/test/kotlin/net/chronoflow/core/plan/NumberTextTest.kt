package net.chronoflow.core.plan

import assertk.assertThat
import assertk.assertions.isEqualTo
import net.chronoflow.core.model.contracts.Language
import org.junit.Test

class NumberTextTest {
    private val rows =
        listOf(
            Triple(0.0, "0", "0"),
            Triple(1.0, "1", "1"),
            Triple(1.5, "1.5", "1,5"),
            Triple(1500.0, "1,500", "1.500"),
            Triple(-1500.0, "-1,500", "-1.500"),
            Triple(1234567.8915, "1,234,567.892", "1.234.567,892"),
            Triple(0.0005, "0", "0"),
            Triple(0.0015, "0.002", "0,002"),
            Triple(2.5, "2.5", "2,5"),
            Triple(1.23456, "1.235", "1,235"),
            Triple(-0.5, "-0.5", "-0,5"),
            Triple(12.0, "12", "12"),
            Triple(999.9995, "1,000", "1.000"),
            Triple(1.0005, "1", "1"),
            Triple(0.1235, "0.124", "0,124"),
            Triple(0.1245, "0.124", "0,124"),
            Triple(-0.0, "-0", "-0"),
            Triple(-0.0004, "-0", "-0"),
            Triple(1.0E15, "1,000,000,000,000,000", "1.000.000.000.000.000"),
            Triple(12345.0, "12,345", "12.345"),
        )

    @Test fun appleTable() {
        val mismatches =
            rows.flatMap { (value, en, nl) ->
                listOfNotNull(
                    formatPlanNumber(value, Language.En).takeIf { it != en }?.let { "en $value: $it" },
                    formatPlanNumber(value, Language.Nl).takeIf { it != nl }?.let { "nl $value: $it" },
                )
            }
        assertThat(mismatches).isEqualTo(emptyList<String>())
    }

    @Test fun nonFiniteValuesDoNotCrash() {
        assertThat(formatPlanNumber(Double.NaN, Language.En)).isEqualTo("NaN")
        assertThat(formatPlanNumber(Double.POSITIVE_INFINITY, Language.En)).isEqualTo("∞")
        assertThat(formatPlanNumber(Double.NEGATIVE_INFINITY, Language.Nl)).isEqualTo("-∞")
    }
}
