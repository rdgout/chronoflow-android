package net.chronoflow.buildlogic.size

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.util.zip.ZipFile
import javax.inject.Inject

abstract class MeasureBundleSizeTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val bundle: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val aapt2: RegularFileProperty

    @get:Classpath
    abstract val bundletoolClasspath: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Inject
    abstract val exec: ExecOperations

    init {
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun measure() {
        val aab = bundle.get().asFile
        val out = outputDir.get().asFile.apply { mkdirs() }

        val libraries =
            ZipFile(aab).use { zip ->
                BundleSize.nativeLibraries(
                    zip.entries().toList().map {
                        it.name
                    },
                    BundleSize.allowedNativeLibraries,
                )
            }
        if (libraries.isNotEmpty()) {
            throw GradleException("Native libraries in the bundle:\n" + libraries.joinToString("\n") { "  $it" })
        }

        val spec = out.resolve("device-spec.json")
        spec.writeText(
            """{"supportedAbis":["arm64-v8a"],"supportedLocales":["en"],"screenDensity":480,"sdkVersion":36}""" + "\n",
        )
        val apks = out.resolve("app.apks")
        runBundletool(
            "build-apks",
            "--bundle=${aab.absolutePath}",
            "--output=${apks.absolutePath}",
            "--device-spec=${spec.absolutePath}",
            "--aapt2=${aapt2.get().asFile.absolutePath}",
            "--overwrite",
        )
        val csv =
            runBundletool("get-size", "total", "--apks=${apks.absolutePath}", "--device-spec=${spec.absolutePath}")

        val bytes = BundleSize.parseGetSizeTotal(csv)
        val verdict = BundleSize.verdict(bytes)
        val line =
            "Bundle size: $bytes bytes (%.2f MB), arm64-v8a/xxhdpi/en: $verdict".format(
                java.util.Locale.ROOT,
                bytes / 1_000_000.0,
            )
        println(line)
        out.resolve("size-report.txt").writeText(line + "\n")
        when (verdict) {
            Verdict.OK -> Unit
            Verdict.WARN -> logger.warn("warning: $line")
            Verdict.FAIL -> throw GradleException(line)
        }
    }

    private fun runBundletool(vararg args: String): String {
        val stdout = ByteArrayOutputStream()
        exec.javaexec {
            classpath = bundletoolClasspath
            mainClass.set("com.android.tools.build.bundletool.BundleToolMain")
            this.args(*args)
            standardOutput = stdout
        }
        return stdout.toString()
    }
}
