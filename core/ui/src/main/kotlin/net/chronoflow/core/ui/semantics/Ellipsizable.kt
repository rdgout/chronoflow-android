package net.chronoflow.core.ui.semantics

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.semantics

/** Marks text whose ellipsis is intended, so the layout guards do not report it as truncation. */
val Ellipsizable = SemanticsPropertyKey<Unit>("cf.ellipsizable")

fun Modifier.ellipsizable(): Modifier = semantics { this[Ellipsizable] = Unit }
