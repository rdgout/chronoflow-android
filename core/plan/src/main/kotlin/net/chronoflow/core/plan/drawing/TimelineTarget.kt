package net.chronoflow.core.plan.drawing

/** One accessibility element of a day, in px. */
class TimelineTarget(
    val kind: Kind,
    val blockId: String?,
    val label: String,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val traversalIndex: Float,
    val testTag: String?,
) {
    enum class Kind { BLOCK, DIVIDER, TICK }
}
