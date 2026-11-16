package net.chronoflow.core.testing.tiers

/** Tier `snapshot`: a Roborazzi reference comparison, run only by `make snapshots`. */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@MustBeDocumented
annotation class Snapshot
