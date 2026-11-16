package net.chronoflow.core.testing.tiers

/** A Macrobenchmark class in `:benchmark`; it never runs in a JVM tier. */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@MustBeDocumented
annotation class Perf
