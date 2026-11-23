package net.chronoflow.core.model.config

enum class ThemeChoice(
    val raw: String,
) {
    System("system"),
    Light("light"),
    Dark("dark"),
    ;

    companion object {
        fun fromRaw(raw: String?): ThemeChoice? = entries.firstOrNull { it.raw == raw }
    }
}
