package net.chronoflow.core.model

import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.CheckStatus

// Not @Serializable: the storage codec belongs to the outbox phase, the wire body is ApiCheckOffInput.
data class CheckOffChange(
    val tripID: String,
    val planInputHash: String,
    val type: BlockType,
    val localDate: String,
    val ordinal: Int,
    val status: CheckStatus?,
)
