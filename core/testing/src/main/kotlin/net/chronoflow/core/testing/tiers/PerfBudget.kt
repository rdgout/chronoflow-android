package net.chronoflow.core.testing.tiers

/** Not a tier: exempts a test from the per-test duration budget. */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@MustBeDocumented
annotation class PerfBudget
