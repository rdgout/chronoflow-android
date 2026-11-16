package net.chronoflow.core.testing.junit

import org.junit.platform.engine.FilterResult
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.support.descriptor.ClassSource
import org.junit.platform.engine.support.descriptor.MethodSource
import org.junit.platform.launcher.PostDiscoveryFilter

class TierDiscoveryFilter : PostDiscoveryFilter {
    private val tier = TierName.parse(System.getProperty("cf.tier"))

    override fun apply(descriptor: TestDescriptor): FilterResult {
        val className =
            descriptor.source
                .map {
                    when (it) {
                        is ClassSource -> it.className
                        is MethodSource -> it.className
                        else -> null
                    }
                }.orElse(null) ?: return FilterResult.included("no class source")
        val clazz =
            try {
                Class.forName(className, false, Thread.currentThread().contextClassLoader)
            } catch (_: ClassNotFoundException) {
                return FilterResult.included("class not loadable")
            } catch (_: LinkageError) {
                return FilterResult.included("class not loadable")
            }
        val classTier = clazz.tierName()
        return if (tier.includes(classTier)) {
            FilterResult.included("cf.tier=$tier")
        } else {
            FilterResult.excluded("cf.tier=$tier excludes @${classTier ?: "untagged"}")
        }
    }
}
