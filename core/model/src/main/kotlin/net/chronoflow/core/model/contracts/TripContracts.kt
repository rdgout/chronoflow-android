package net.chronoflow.core.model.contracts

import kotlinx.serialization.Serializable
import net.chronoflow.core.model.api.ApiJson

typealias CheckKey = CheckKeyView
typealias CheckStateView = CheckState
typealias CheckMarkView = CheckMark
typealias TripCardProgress = TripProgress
typealias GutterTickView = TickView

@Serializable
data class TripCheckResult(
    val checkState: CheckState,
    val detailVersion: String,
)

@Serializable
data class TripListResponse(
    val trips: List<TripListItem>,
)

class TripDetailResponse private constructor(
    val data: ByteArray,
    val detail: TripDetail,
) {
    override fun equals(other: Any?): Boolean =
        other is TripDetailResponse && data.contentEquals(other.data) && detail == other.detail

    override fun hashCode(): Int = 31 * data.contentHashCode() + detail.hashCode()

    override fun toString(): String = "TripDetailResponse(detail=$detail)"

    companion object {
        fun decode(data: ByteArray): TripDetailResponse {
            val cleaned = ProfilePreferences.cleanLegacyJSON(data)
            val detail = ApiJson.response.decodeFromString(TripDetail.serializer(), cleaned.decodeToString())
            return TripDetailResponse(cleaned, detail)
        }

        fun encoding(detail: TripDetail): TripDetailResponse =
            decode(ApiJson.response.encodeToString(TripDetail.serializer(), detail).encodeToByteArray())
    }
}
