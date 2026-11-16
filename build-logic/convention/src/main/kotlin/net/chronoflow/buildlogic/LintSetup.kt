package net.chronoflow.buildlogic

import com.android.build.api.dsl.Lint
import org.gradle.api.Project

internal fun Project.configureLint(lint: Lint) {
    lint.apply {
        abortOnError = true
        warningsAsErrors = true
        lintConfig = rootProject.file("lint.xml")
    }
}
