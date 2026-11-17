package net.chronoflow.core.designsystem.layout

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class BoardMetrics(
    vararg val boards: String,
)
