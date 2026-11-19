package net.chronoflow.feature.launch.welcome

import net.chronoflow.feature.launch.LaunchController
import net.chronoflow.feature.launch.model.LaunchRect

object WelcomeLaunchDiagnostics {
    fun measuredMoonTarget(controller: LaunchController): LaunchRect? = controller.coordinator?.state?.moonTarget
}
