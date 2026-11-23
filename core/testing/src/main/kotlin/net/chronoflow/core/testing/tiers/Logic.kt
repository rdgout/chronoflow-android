package net.chronoflow.core.testing.tiers

/** Tier `quick`: a Robolectric test of logic that runs on one SDK and is cheap enough for `make quick`. */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@MustBeDocumented
annotation class Logic
