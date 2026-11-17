package net.chronoflow.feature.launch.model

import com.sun.management.ThreadMXBean
import java.lang.management.ManagementFactory

internal object LaunchFrameAllocationProbe {
    @JvmStatic fun main(args: Array<String>) {
        val sampler = LaunchFrameSampler(LaunchTimeline.opening(LaunchSize(390.0, 844.0), false))
        val out = LaunchFrame()
        val bean = ManagementFactory.getThreadMXBean() as ThreadMXBean
        bean.isThreadAllocatedMemoryEnabled = true
        val id = Thread.currentThread().threadId()
        repeat(1000) { sampler.sampleInto(it / 1000.0, out) }
        val baselineStart = bean.getThreadAllocatedBytes(id)
        val baselineEnd = bean.getThreadAllocatedBytes(id)
        val start = bean.getThreadAllocatedBytes(id)
        var i = 0
        while (i < 10000) {
            sampler.sampleInto(i / 10000.0, out)
            i++
        }
        val end = bean.getThreadAllocatedBytes(id)
        check(end - start <= baselineEnd - baselineStart) {
            "allocated ${end - start} bytes; baseline ${baselineEnd - baselineStart}"
        }
        check(out.moonY < 422)
    }
}
