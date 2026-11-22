package net.chronoflow.core.testing.tiers

/** An emulator-only accessibility/verification case, outside the commit gate's smoke set. */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Audit
