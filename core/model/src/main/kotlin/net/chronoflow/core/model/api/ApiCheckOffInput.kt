package net.chronoflow.core.model.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.CheckStatus

// No default on status: the server requires the key, so ApiJson.request writes an explicit null.
@Serializable
data class ApiCheckOffInput(
    @SerialName("plan_input_hash") val planInputHash: String,
    val type: BlockType,
    @SerialName("local_date") val localDate: String,
    val ordinal: Int,
    val status: CheckStatus?,
)
