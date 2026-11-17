package net.chronoflow.feature.launch

class LaunchFrameLog {
    val sampledNanos = LongArray(512)
    val wallNanos = LongArray(512)
    var count = 0L
        private set

    fun record(
        sampled: Long,
        wall: Long,
    ) {
        val index = (count % sampledNanos.size).toInt()
        sampledNanos[index] = sampled
        wallNanos[index] = wall
        count++
    }
}
