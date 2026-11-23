package net.chronoflow.buildlogic.signing

import java.io.File

class UploadSigning(
    val storeFile: File,
    val storePassword: String,
    val keyAlias: String,
    val keyPassword: String,
)

object UploadSigningResolver {
    val KEYS =
        listOf("CF_UPLOAD_STORE_FILE", "CF_UPLOAD_STORE_PASSWORD", "CF_UPLOAD_KEY_ALIAS", "CF_UPLOAD_KEY_PASSWORD")

    fun resolve(
        values: Map<String, String?>,
        repoRoot: File,
        home: File,
        exists: (File) -> Boolean,
    ): UploadSigning? {
        val present = values.filterValues { !it.isNullOrBlank() }
        if (KEYS.none { it in present }) return null
        val missing = KEYS.filter { it !in present }
        require(missing.isEmpty()) { "CF_UPLOAD_* incomplete: missing ${missing.joinToString(",")}" }
        val path = requireNotNull(present[KEYS[0]])
        val file = (if (path.startsWith("~/")) File(home, path.removePrefix("~/")) else File(path)).canonicalFile
        require(file.isAbsolute && File(if (path.startsWith("~/")) home.path else path).isAbsolute) {
            "CF_UPLOAD_STORE_FILE must be absolute: $path"
        }
        require(!file.toPath().startsWith(repoRoot.canonicalFile.toPath())) {
            "CF_UPLOAD_STORE_FILE must be outside the repository: $file"
        }
        require(exists(file)) { "CF_UPLOAD_STORE_FILE does not exist: $file" }
        return UploadSigning(
            file,
            requireNotNull(present[KEYS[1]]),
            requireNotNull(present[KEYS[2]]),
            requireNotNull(present[KEYS[3]]),
        )
    }
}
