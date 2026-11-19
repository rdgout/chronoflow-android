package net.chronoflow.core.plan

val a = java.time.format.DateTimeFormatter.ofPattern("HH:mm", java.util.Locale.ROOT)
val b = java.text.NumberFormat.getInstance(java.util.Locale.ROOT)
val c = java.text.DecimalFormatSymbols(java.util.Locale.ROOT)

fun d(locale: java.util.Locale) = java.time.format.DateTimeFormatter.ofPattern("HH:mm", locale)

fun e(locale: java.util.Locale) = java.text.NumberFormat.getInstance(locale)

fun f(locale: java.util.Locale) = java.text.DecimalFormatSymbols(locale)

val g = java.time.format.DateTimeFormatter.ofPattern("HH:mm").withLocale(java.util.Locale.ROOT)
