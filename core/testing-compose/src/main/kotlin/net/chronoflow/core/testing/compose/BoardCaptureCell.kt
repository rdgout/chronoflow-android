package net.chronoflow.core.testing.compose

import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.DeviceProfiles

data class BoardCaptureCell(
    val profile: DeviceProfile,
    val language: AppLanguage,
    val dark: Boolean,
) {
    val suffix: String get() = "${profile.name}_${language.tag}_${if (dark) "dark" else "light"}"

    override fun toString(): String = suffix

    companion object {
        val phone: List<BoardCaptureCell> =
            AppLanguage.entries.flatMap { language ->
                listOf(false, true).map { dark -> BoardCaptureCell(DeviceProfiles.pixel8, language, dark) }
            }
        val wide: List<BoardCaptureCell> = phone.map { it.copy(profile = DeviceProfiles.foldInner) }
        val large: List<BoardCaptureCell> =
            listOf(
                BoardCaptureCell(DeviceProfiles.largeFont, AppLanguage.Nl, false),
                BoardCaptureCell(DeviceProfiles.largeFont, AppLanguage.En, true),
            )
        val standard: List<BoardCaptureCell> = phone + listOf(wide[2], wide[1]) + large
        val phoneWithWideSibling: List<BoardCaptureCell> = phone + large
    }
}
