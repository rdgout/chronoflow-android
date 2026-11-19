package net.chronoflow.core.model.api

import net.chronoflow.core.model.DataFailure

object TripWizardErrors {
    fun map(
        failure: DataFailure,
        outboundCount: Int? = null,
    ): DataFailure {
        if (failure !is DataFailure.Validation) return failure
        val fields = failure.fields
        if ("review_expired" in fields) return DataFailure.ReviewExpired
        if (fields["credits"]?.key == "Not enough credits") return DataFailure.NotEnoughCredits
        if (fields["review"]?.key == "Try saving again shortly.") return DataFailure.SaveCapacity
        val mapped = linkedMapOf<String, net.chronoflow.core.model.FieldMessage>()
        fields.forEach { (key, value) ->
            mapped.putIfAbsent(outboundCount?.let { journeyPath(key, it) } ?: key, value)
        }
        return if (fields.keys.any { it == "profile" || it.startsWith("profile.") }) {
            DataFailure.ProfileInvalid(mapped)
        } else {
            DataFailure.Validation(mapped, failure.status)
        }
    }

    fun journeyPath(
        path: String,
        outboundCount: Int,
    ): String {
        val parts = path.split('.').filter { it.isNotEmpty() }
        val index = parts.getOrNull(2)?.toIntOrNull()
        if (parts.size >= 3 && parts[0] in listOf("outbound", "return") && parts[1] == "legs" && index != null) {
            val flat = index + if (parts[0] == "return") outboundCount else 0
            val stay = parts.getOrNull(3) == "stay"
            return (
                listOf(if (stay) "stays" else "legs", flat.toString()) +
                    parts.drop(if (stay) 4 else 3).map(::camel)
            ).joinToString(".")
        }
        if (path == "override.use") return "override.enabled"
        if (parts.isEmpty()) return path
        return (listOf(top[parts[0]] ?: parts[0]) + parts.drop(1).map(::camel)).joinToString(".")
    }

    fun camel(key: String): String {
        val parts = key.split('_')
        return parts[0] + parts.drop(1).joinToString("") { it.take(1).uppercase() + it.drop(1) }
    }

    private val top =
        mapOf(
            "trip_end_kind" to "end.kind",
            "trip_end_nights" to "end.nights",
            "trip_end_check_out_date" to "end.checkOutDate",
            "preparation_days" to "preparation.days",
            "latest_wake" to "preparation.latestWake",
            "pre_departure_busy_answered" to "preparation.busyAnswered",
            "pre_departure_busy_intervals" to "preparation.busyIntervals",
            "feasible_night_bed" to "preparation.feasibleNightBed",
            "feasible_night_wake" to "preparation.feasibleNightWake",
            "fast_delay_opt_in" to "preparation.fastDelayOptIn",
        )
}
