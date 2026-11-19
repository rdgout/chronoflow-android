package net.chronoflow.core.ui.plan

import net.chronoflow.core.model.contracts.Priority
import net.chronoflow.core.plan.priorityKey
import net.chronoflow.core.plan.step.legendDescription
import net.chronoflow.core.plan.step.legendGroups
import net.chronoflow.core.plan.step.priorityDescription
import net.chronoflow.core.plan.stepLabelKey
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.core.ui.l10n.generated.CopyIndex
import net.chronoflow.core.ui.l10n.generated.CopyTable
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@Logic
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36])
class LegendCopyResolutionTest(
    private val language: String,
    private val key: String,
) {
    @Test fun keyResolves() {
        val app = RuntimeEnvironment.getApplication()
        val config =
            android.content.res.Configuration(app.resources.configuration).apply {
                setLocale(java.util.Locale.forLanguageTag(language))
            }
        val context = app.createConfigurationContext(config)
        assertTrue(context.getString(checkNotNull(CopyIndex.find(CopyTable.Screens, key)).resId).isNotBlank())
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}")
        fun cases(): List<Array<Any>> {
            val priorities = listOf(Priority.Core, Priority.Recommended, Priority.Optional, Priority.Info)
            val keys =
                legendGroups.flatMap { it.second }.flatMap {
                    listOf(
                        checkNotNull(stepLabelKey(it)),
                        legendDescription(it),
                    )
                } +
                    priorities.flatMap { listOf(priorityKey(it), priorityDescription(it)) }
            return listOf("en", "nl").flatMap { language -> keys.distinct().map { arrayOf<Any>(language, it) } }
        }
    }
}
