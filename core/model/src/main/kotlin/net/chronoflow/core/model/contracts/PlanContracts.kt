package net.chronoflow.core.model.contracts

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

sealed interface BlockParams {
    data class Flight(
        val value: FlightParamsView,
    ) : BlockParams

    data class Layover(
        val value: LayoverParamsView,
    ) : BlockParams

    data class Transfer(
        val value: TransferParamsView,
    ) : BlockParams

    data class MovementBreak(
        val value: MovementBreakParamsView,
    ) : BlockParams

    data class ZoneChange(
        val value: ZoneChangeParamsView,
    ) : BlockParams

    data class Sleep(
        val value: SleepParamsView,
    ) : BlockParams

    data class Nap(
        val value: NapParamsView,
    ) : BlockParams

    data class Rest(
        val value: RestParamsView,
    ) : BlockParams

    data class AvoidLight(
        val value: AvoidLightParamsView,
    ) : BlockParams

    data class SeekLight(
        val value: SeekLightParamsView,
    ) : BlockParams

    data class LightOK(
        val value: LightOkParamsView,
    ) : BlockParams

    data class LightNeutral(
        val value: LightNeutralParamsView,
    ) : BlockParams

    data class CaffeineServing(
        val value: CaffeineServingParamsView,
    ) : BlockParams

    data class CaffeineOK(
        val value: CaffeineOkParamsView,
    ) : BlockParams

    data class CaffeineRoutine(
        val value: CaffeineRoutineParamsView,
    ) : BlockParams

    data class NoCaffeine(
        val value: NoCaffeineParamsView,
    ) : BlockParams

    data class Note(
        val value: NoteBlockParamsView,
    ) : BlockParams

    data class CaffeineServingDrinks(
        val value: CaffeineServingDrinksParamsView,
    ) : BlockParams

    data class CaffeineOKDrinks(
        val value: CaffeineOkDrinksParamsView,
    ) : BlockParams

    data class NoCaffeineDrinks(
        val value: NoCaffeineDrinksParamsView,
    ) : BlockParams

    data class Unknown(
        val value: JsonElement,
    ) : BlockParams

    fun encode(json: Json): JsonElement =
        when (this) {
            is Flight -> json.encodeToJsonElement(FlightParamsView.serializer(), value)
            is Layover -> json.encodeToJsonElement(LayoverParamsView.serializer(), value)
            is Transfer -> json.encodeToJsonElement(TransferParamsView.serializer(), value)
            is MovementBreak -> value
            is ZoneChange -> json.encodeToJsonElement(ZoneChangeParamsView.serializer(), value)
            is Sleep -> json.encodeToJsonElement(SleepParamsView.serializer(), value)
            is Nap -> json.encodeToJsonElement(NapParamsView.serializer(), value)
            is Rest -> json.encodeToJsonElement(RestParamsView.serializer(), value)
            is AvoidLight -> json.encodeToJsonElement(AvoidLightParamsView.serializer(), value)
            is SeekLight -> json.encodeToJsonElement(SeekLightParamsView.serializer(), value)
            is LightOK -> json.encodeToJsonElement(LightOkParamsView.serializer(), value)
            is LightNeutral -> json.encodeToJsonElement(LightNeutralParamsView.serializer(), value)
            is CaffeineServing -> json.encodeToJsonElement(CaffeineServingParamsView.serializer(), value)
            is CaffeineOK -> json.encodeToJsonElement(CaffeineOkParamsView.serializer(), value)
            is CaffeineRoutine -> json.encodeToJsonElement(CaffeineRoutineParamsView.serializer(), value)
            is NoCaffeine -> json.encodeToJsonElement(NoCaffeineParamsView.serializer(), value)
            is Note -> json.encodeToJsonElement(NoteBlockParamsView.serializer(), value)
            is CaffeineServingDrinks -> json.encodeToJsonElement(CaffeineServingDrinksParamsView.serializer(), value)
            is CaffeineOKDrinks -> json.encodeToJsonElement(CaffeineOkDrinksParamsView.serializer(), value)
            is NoCaffeineDrinks -> json.encodeToJsonElement(NoCaffeineDrinksParamsView.serializer(), value)
            is Unknown -> value
        }

    companion object {
        fun decode(
            type: BlockType,
            element: JsonElement,
            json: Json,
        ): BlockParams =
            when (type) {
                BlockType.Flight -> {
                    Flight(json.decodeFromJsonElement(FlightParamsView.serializer(), element))
                }

                BlockType.Layover -> {
                    Layover(json.decodeFromJsonElement(LayoverParamsView.serializer(), element))
                }

                BlockType.Transfer -> {
                    Transfer(json.decodeFromJsonElement(TransferParamsView.serializer(), element))
                }

                BlockType.ZoneChange -> {
                    ZoneChange(json.decodeFromJsonElement(ZoneChangeParamsView.serializer(), element))
                }

                BlockType.Sleep -> {
                    Sleep(json.decodeFromJsonElement(SleepParamsView.serializer(), element))
                }

                BlockType.Nap -> {
                    Nap(json.decodeFromJsonElement(NapParamsView.serializer(), element))
                }

                BlockType.Rest -> {
                    Rest(json.decodeFromJsonElement(RestParamsView.serializer(), element))
                }

                BlockType.AvoidLight -> {
                    AvoidLight(json.decodeFromJsonElement(AvoidLightParamsView.serializer(), element))
                }

                BlockType.SeekLight -> {
                    SeekLight(json.decodeFromJsonElement(SeekLightParamsView.serializer(), element))
                }

                BlockType.LightOK -> {
                    LightOK(json.decodeFromJsonElement(LightOkParamsView.serializer(), element))
                }

                BlockType.LightNeutral -> {
                    LightNeutral(json.decodeFromJsonElement(LightNeutralParamsView.serializer(), element))
                }

                BlockType.CaffeineRoutine -> {
                    CaffeineRoutine(json.decodeFromJsonElement(CaffeineRoutineParamsView.serializer(), element))
                }

                BlockType.Note -> {
                    Note(json.decodeFromJsonElement(NoteBlockParamsView.serializer(), element))
                }

                BlockType.MovementBreak -> {
                    MovementBreak(
                        element as? JsonObject ?: throw SerializationException(
                            "movement_break params must be an object",
                        ),
                    )
                }

                BlockType.CaffeineServing -> {
                    if (element.hasObject("amount")) {
                        CaffeineServingDrinks(
                            json.decodeFromJsonElement(CaffeineServingDrinksParamsView.serializer(), element),
                        )
                    } else {
                        CaffeineServing(json.decodeFromJsonElement(CaffeineServingParamsView.serializer(), element))
                    }
                }

                BlockType.CaffeineOK -> {
                    if (element.hasObject("amount")) {
                        CaffeineOKDrinks(json.decodeFromJsonElement(CaffeineOkDrinksParamsView.serializer(), element))
                    } else {
                        CaffeineOK(json.decodeFromJsonElement(CaffeineOkParamsView.serializer(), element))
                    }
                }

                BlockType.NoCaffeine -> {
                    if ((element.field("exception") as? JsonObject)?.get("amount") is JsonObject) {
                        NoCaffeineDrinks(json.decodeFromJsonElement(NoCaffeineDrinksParamsView.serializer(), element))
                    } else {
                        NoCaffeine(json.decodeFromJsonElement(NoCaffeineParamsView.serializer(), element))
                    }
                }

                is BlockType.Unknown -> {
                    Unknown(element)
                }
            }

        private fun JsonElement.field(key: String): JsonElement? = (this as? JsonObject)?.get(key)

        private fun JsonElement.hasObject(key: String): Boolean = field(key) is JsonObject
    }
}

val BlockView.isSupported: Boolean
    get() = type !is BlockType.Unknown && lane !is Lane.Unknown

val PlanView.renderableBlocks: List<BlockView>
    get() = blocks.filter { it.isSupported }
