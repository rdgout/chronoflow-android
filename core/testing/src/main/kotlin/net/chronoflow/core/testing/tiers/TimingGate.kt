package net.chronoflow.core.testing.tiers

/** Tier `timing`: a plain JVM wall-clock budget test, run only by the serial `timingTest` task and exempt from the duration budget. */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@MustBeDocumented
annotation class TimingGate
