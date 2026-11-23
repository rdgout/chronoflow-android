package net.chronoflow.core.testing.tiers

/** Tier `layout`: a Robolectric layout or geometry guard, run by `make check` and `make test`. */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@MustBeDocumented
annotation class Layout
