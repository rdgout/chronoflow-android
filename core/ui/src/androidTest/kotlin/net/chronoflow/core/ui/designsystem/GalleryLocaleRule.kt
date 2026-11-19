package net.chronoflow.core.ui.designsystem

import android.app.LocaleManager
import android.os.LocaleList
import androidx.test.platform.app.InstrumentationRegistry
import net.chronoflow.core.model.config.AppLanguage
import org.junit.rules.ExternalResource

internal class GalleryLocaleRule(
    private val language: AppLanguage,
) : ExternalResource() {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val manager = instrumentation.targetContext.getSystemService(LocaleManager::class.java)
    private lateinit var previous: LocaleList

    override fun before() {
        previous = manager.applicationLocales
        manager.applicationLocales = LocaleList.forLanguageTags(language.tag)
        instrumentation.waitForIdleSync()
    }

    override fun after() {
        manager.applicationLocales = previous
        instrumentation.waitForIdleSync()
    }
}
