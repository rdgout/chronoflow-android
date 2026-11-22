package net.chronoflow.core.reminders

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.BlockParams
import net.chronoflow.core.model.contracts.BlockReasonTagsView
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.BlockView
import net.chronoflow.core.model.contracts.CheckKeyView
import net.chronoflow.core.model.contracts.CopyLine
import net.chronoflow.core.model.contracts.Lane
import net.chronoflow.core.model.contracts.Priority
import net.chronoflow.core.model.contracts.SleepParamsView
import net.chronoflow.core.model.contracts.TimelinePresentationView
import net.chronoflow.core.plan.planDate
import net.chronoflow.core.testing.Fixtures
import org.junit.Test

class ReminderSelectionTests {
    @Test fun allRNSelectionVectors() {
        assertThat(vectors.size).isEqualTo(110)
        val mismatches =
            vectors.mapIndexedNotNull { index, vector ->
                val actual = reminderDrafts(vector.blocks, vector.settings, vector.now, vector.options)
                if (actual == vector.expected) {
                    null
                } else {
                    "$index ${vector.name}: expected ${vector.expected}, got $actual"
                }
            }
        assertThat(mismatches).isEmpty()
    }

    companion object {
        internal val vectors by lazy {
            ApiJson.response
                .parseToJsonElement(Fixtures.text("reminders/selection.json"))
                .jsonObject
                .getValue("cases")
                .jsonArray
                .map { decodeVector(it.jsonObject) }
        }
    }
}

internal data class SelectionVector(
    val name: String,
    val blocks: List<BlockView>,
    val settings: ReminderSettings,
    val now: java.time.Instant,
    val options: ReminderSelectionOptions,
    val expected: List<ReminderDraft>,
)

private fun JsonObject.text(key: String): String = getValue(key).jsonPrimitive.content

private fun JsonObject.date(key: String) = checkNotNull(planDate(text(key)))

private fun decodeVector(value: JsonObject): SelectionVector {
    val options = value.getValue("options").jsonObject
    return SelectionVector(
        value.text("test"),
        value
            .getValue(
                "blocks",
            ).jsonArray
            .map { ApiJson.response.decodeFromJsonElement(ParityBlock.serializer(), it).value() },
        checkNotNull(ReminderSettings.decode(value.getValue("settings").toString())),
        value.date("now"),
        ReminderSelectionOptions(
            coalesceMinutes = options["coalesceMinutes"]?.jsonPrimitive?.int ?: 15,
            dailyCap = options["dailyCap"]?.jsonPrimitive?.int ?: 6,
            expiryGraceMinutes = options["expiryGraceMinutes"]?.jsonPrimitive?.int ?: 20,
            sentMemberKeys =
                options["sentMemberKeys"]
                    ?.jsonObject
                    .orEmpty()
                    .filterValues { it.jsonPrimitive.boolean }
                    .keys,
            sentCountsByDay =
                options["sentCountsByDay"]?.jsonObject.orEmpty().mapValues {
                    it.value.jsonPrimitive.int
                },
            timeline =
                options["timeline"]?.jsonObject?.let { timeline ->
                    TimelinePresentationView(
                        emptyList(),
                        timeline.getValue("blockReasons").jsonArray.map {
                            ApiJson.response.decodeFromJsonElement(BlockReasonTagsView.serializer(), it)
                        },
                        emptyList(),
                    )
                },
        ),
        value.getValue("expected").jsonArray.map { element ->
            val draft = element.jsonObject
            ReminderDraft(
                draft.getValue("members").jsonArray.map { memberElement ->
                    val member = memberElement.jsonObject
                    ReminderMember(
                        checkNotNull(ReminderKind.of(member.text("kind"))),
                        BlockType.of(member.text("blockType")),
                        member.text("localDate"),
                        member.getValue("ordinal").jsonPrimitive.int,
                        member.date("sendAt"),
                        member.date("blockStartUtc"),
                        member.text("displayTz"),
                        member.date("expiresAt"),
                    )
                },
                draft.date("sendAt"),
                draft.date("expiresAt"),
                draft.date("blockStartUtc"),
                draft.text("displayTz"),
                Priority.of(draft.text("priority")),
            )
        },
    )
}

@Serializable
private data class ParityBlock(
    val id: String,
    val lane: Lane,
    val type: BlockType,
    val startUtc: String,
    val endUtc: String,
    val displayTz: String,
    val priority: Priority,
    val text: CopyLine,
    val params: JsonElement,
    val actionable: Boolean,
    val checkKey: CheckKeyView? = null,
) {
    fun value(): BlockView {
        // RN's synthetic excluded blocks omit presenter fields; selection only reads sleep params.
        val typed =
            if (type == BlockType.Sleep) {
                BlockParams.Sleep(ApiJson.response.decodeFromJsonElement(SleepParamsView.serializer(), params))
            } else {
                BlockParams.Unknown(params)
            }
        return BlockView(
            id,
            lane,
            type,
            startUtc,
            endUtc,
            displayTz,
            null,
            priority,
            text,
            null,
            typed,
            actionable,
            checkKey,
        )
    }
}
