package net.chronoflow.core.testing.tiers

/** Tier `capture`: a board capture, run only by `make captures`. */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@MustBeDocumented
annotation class Capture
