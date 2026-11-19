package net.chronoflow.core.plan.trippage

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.TimeFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class PlanPreferencesTest {
    private fun parse(raw: String) = planPreferences(Json.parseToJsonElement(raw).jsonObject)

    @Test fun missingPreferencesUseDefaults() {
        assertEquals(PlanPreferences(), planPreferences(null))
    }

    @Test fun twelveHourStringIsRecognized() {
        assertEquals(TimeFormat.Value12, parse("""{"timeFormat":"12"}""").timeFormat)
    }

    @Test fun twentyFourHourStringIsRecognized() {
        assertEquals(TimeFormat.Value24, parse("""{"timeFormat":"24"}""").timeFormat)
    }

    @Test fun numbersAreNotTimeFormatStrings() {
        assertEquals(TimeFormat.Value24, parse("""{"timeFormat":12}""").timeFormat)
    }

    @Test fun unknownTimeFormatIsPreserved() {
        assertEquals(TimeFormat.Unknown("x"), parse("""{"timeFormat":"x"}""").timeFormat)
    }

    @Test fun hiddenCategoriesKeepUnknownStringsOrderAndDuplicates() {
        assertEquals(
            listOf(BlockType.CaffeineServing, BlockType.SeekLight, BlockType.Unknown("bogus"), BlockType.SeekLight),
            parse("""{"hiddenCategories":["caffeine_serving",3,"seek_light","bogus","seek_light"]}""").hiddenCategories,
        )
    }

    @Test fun nonArrayCategoriesUseDefaults() {
        assertEquals(emptyList<BlockType>(), parse("""{"hiddenCategories":"caffeine_serving"}""").hiddenCategories)
    }
}
