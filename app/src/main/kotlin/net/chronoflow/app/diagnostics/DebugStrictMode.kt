package net.chronoflow.app.diagnostics

import android.app.Application
import android.os.StrictMode
import android.os.strictmode.DiskReadViolation
import android.os.strictmode.DiskWriteViolation
import android.os.strictmode.NetworkViolation
import android.os.strictmode.Violation

class LaunchPathViolation(
    cause: Violation,
) : RuntimeException("Main-thread disk or network work on the launch path", cause)

internal object DebugStrictMode {
    fun install(
        app: Application,
        launchPath: LaunchPath,
    ) {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy
                .Builder()
                .detectAll()
                .penaltyLog()
                .penaltyListener(app.mainExecutor) { violation ->
                    val isDiskOrNetwork =
                        violation is DiskReadViolation ||
                            violation is DiskWriteViolation ||
                            violation is NetworkViolation
                    val frames = violation.stackTrace.map { it.className }
                    if (LaunchPathViolationPolicy.shouldCrash(launchPath.isActive, isDiskOrNetwork, frames)) {
                        throw LaunchPathViolation(violation)
                    }
                }.build(),
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy
                .Builder()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .build(),
        )
    }
}
