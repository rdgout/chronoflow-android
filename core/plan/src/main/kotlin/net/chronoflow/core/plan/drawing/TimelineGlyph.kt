package net.chronoflow.core.plan.drawing

/** The lucide glyphs the timeline draws (lucide-react 0.511.0, the boards' version), by ordinal in `GLYPH` ops. */
enum class TimelineGlyph(
    val lucideName: String,
) {
    MOON("moon"),
    BED("bed"),
    ARMCHAIR("armchair"),
    SUN("sun"),
    GLASSES("glasses"),
    CLOUD_SUN("cloud-sun"),
    LAMP("lamp"),
    COFFEE("coffee"),
    PLANE("plane"),
    TIMER("timer"),
    ARROW_LEFT_RIGHT("arrow-left-right"),
    FOOTPRINTS("footprints"),
    GLOBE("globe"),
    INFO("info"),
    CHECK("check"),
    SKIP_FORWARD("skip-forward"),
    ;

    companion object {
        val all: Array<TimelineGlyph> = entries.toTypedArray()
    }
}
