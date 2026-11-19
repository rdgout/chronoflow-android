package net.chronoflow.feature.trip.page

interface PageIndex {
    val summary: Int
    val days: Int
    val end: Int
    val hidden: Int?
    val itemCount: Int

    fun headerIndex(dayId: String): Int

    fun dayAt(index: Int): String?
}
