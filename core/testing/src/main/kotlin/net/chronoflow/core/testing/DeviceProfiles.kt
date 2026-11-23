package net.chronoflow.core.testing

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.roundToInt

data class SafeInsetsDp(
    val top: Float,
    val left: Float,
    val bottom: Float,
    val right: Float,
)

data class DeviceProfile(
    val name: String,
    val widthDp: Int,
    val heightDp: Int,
    val density: Float,
    val fontScale: Float,
    val navigation: String,
    val insets: SafeInsetsDp,
    val source: String,
) {
    val densityDpi: Int get() = (density * 160).roundToInt()
    val isLandscape: Boolean get() = widthDp > heightDp
}

object DeviceProfiles {
    val all: List<DeviceProfile> by lazy { load() }

    operator fun get(name: String): DeviceProfile =
        all.firstOrNull { it.name == name } ?: throw IllegalArgumentException("unknown device profile '$name'")

    val small: DeviceProfile get() = get("small")
    val pixel8: DeviceProfile get() = get("pixel8")
    val foldInner: DeviceProfile get() = get("foldInner")
    val landscapeSideNav: DeviceProfile get() = get("landscapeSideNav")
    val largeFont: DeviceProfile get() = get("largeFont")

    private fun load(): List<DeviceProfile> {
        val text =
            DeviceProfiles::class.java.getResourceAsStream("/profiles.json")?.use { it.readBytes().decodeToString() }
                ?: throw IllegalStateException("profiles.json is missing from the classpath")
        val raw =
            Json
                .parseToJsonElement(text)
                .jsonObject
                .getValue("profiles")
                .jsonObject
        val resolved = LinkedHashMap<String, DeviceProfile>()
        for ((name, element) in raw) {
            resolved[name] = build(name, element.jsonObject, raw, resolved)
        }
        return resolved.values.toList()
    }

    private fun JsonObject.int(
        profile: String,
        field: String,
    ): Int =
        this[field]?.jsonPrimitive?.intOrNull
            ?: throw IllegalStateException("profile '$profile' is missing field '$field'")

    private fun JsonObject.float(
        profile: String,
        field: String,
    ): Float =
        this[field]?.jsonPrimitive?.floatOrNull
            ?: throw IllegalStateException("profile '$profile' is missing field '$field'")

    private fun JsonObject.text(
        profile: String,
        field: String,
    ): String =
        this[field]?.jsonPrimitive?.contentOrNull
            ?: throw IllegalStateException("profile '$profile' is missing field '$field'")

    private fun build(
        name: String,
        obj: JsonObject,
        raw: JsonObject,
        resolved: Map<String, DeviceProfile>,
    ): DeviceProfile {
        val base = obj["basedOn"]?.jsonPrimitive?.contentOrNull
        if (base != null) {
            val parent =
                resolved[base] ?: throw IllegalStateException("profile '$name' is based on unknown or later '$base'")
            return parent.copy(name = name, fontScale = obj.float(name, "fontScale"), source = "based on $base")
        }
        val density = obj.float(name, "density")
        val insets =
            when {
                obj["insetsFrom"] != null -> {
                    val from = obj.text(name, "insetsFrom")
                    (
                        resolved[from] ?: throw IllegalStateException(
                            "profile '$name' copies insets of unknown '$from'",
                        )
                    ).insets
                }

                obj["insetsPx"] != null -> {
                    val px = obj.getValue("insetsPx").jsonObject
                    SafeInsetsDp(
                        px.float(name, "top") / density,
                        px.float(name, "left") / density,
                        px.float(name, "bottom") / density,
                        px.float(name, "right") / density,
                    )
                }

                else -> {
                    val dp =
                        (
                            obj["insetsDp"]
                                ?: throw IllegalStateException("profile '$name' is missing field 'insetsDp'")
                        ).jsonObject
                    SafeInsetsDp(
                        dp.float(name, "top"),
                        dp.float(name, "left"),
                        dp.float(name, "bottom"),
                        dp.float(name, "right"),
                    )
                }
            }
        return DeviceProfile(
            name = name,
            widthDp = obj.int(name, "widthDp"),
            heightDp = obj.int(name, "heightDp"),
            density = density,
            fontScale = obj.float(name, "fontScale"),
            navigation = obj.text(name, "navigation"),
            insets = insets,
            source = obj.text(name, "source"),
        )
    }
}
