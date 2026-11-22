package net.chronoflow.buildlogic.signing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.File

class UploadSigningTest {
    private val root = File("/tmp/cf-repo")
    private val home = File("/tmp/cf-home")
    private val values =
        UploadSigningResolver.KEYS
            .zip(
                listOf("/tmp/cf-keys/upload.p12", "store-secret", "upload", "key-secret"),
            ).toMap()

    private fun resolve(
        input: Map<String, String?> = values,
        exists: Boolean = true,
    ) = UploadSigningResolver.resolve(input, root, home) { exists }

    private fun failure(
        input: Map<String, String?> = values,
        exists: Boolean = true,
    ): String = assertThrows(IllegalArgumentException::class.java) { resolve(input, exists) }.message!!

    @Test fun noneSetIsUnsigned() {
        assertNull(resolve(emptyMap()))
    }

    @Test fun allSetResolvesOutsideFileAndCredentials() {
        val signing = requireNotNull(resolve())
        assertEquals(File(values.getValue("CF_UPLOAD_STORE_FILE")).canonicalFile, signing.storeFile)
        assertEquals("store-secret", signing.storePassword)
        assertEquals("upload", signing.keyAlias)
        assertEquals("key-secret", signing.keyPassword)
    }

    @Test fun tildeExpandsHome() {
        assertEquals(
            File(home, "k.p12").canonicalFile,
            resolve(values + ("CF_UPLOAD_STORE_FILE" to "~/k.p12"))!!.storeFile,
        )
    }

    @Test fun partialNamesMissingKeysInOrder() {
        assertEquals(
            "CF_UPLOAD_* incomplete: missing CF_UPLOAD_KEY_ALIAS,CF_UPLOAD_KEY_PASSWORD",
            failure(
                values.filterKeys {
                    it in
                        UploadSigningResolver.KEYS.take(2)
                },
            ),
        )
    }

    @Test fun blankPasswordIsMissing() {
        assertEquals(
            "CF_UPLOAD_* incomplete: missing CF_UPLOAD_STORE_PASSWORD",
            failure(values + ("CF_UPLOAD_STORE_PASSWORD" to " ")),
        )
    }

    @Test fun missingFileFails() {
        assertEquals(
            "CF_UPLOAD_STORE_FILE does not exist: ${File(values.getValue("CF_UPLOAD_STORE_FILE")).canonicalFile}",
            failure(exists = false),
        )
    }

    @Test fun repositoryFileFails() {
        assertEquals(
            "CF_UPLOAD_STORE_FILE must be outside the repository: ${File(root, "app/upload.p12").canonicalFile}",
            failure(values + ("CF_UPLOAD_STORE_FILE" to File(root, "app/upload.p12").path)),
        )
    }

    @Test fun environmentOnlyValuesResolveTheSame() {
        assertNotNull(resolve(UploadSigningResolver.KEYS.associateWith { values[it] }))
    }

    @Test fun relativePathFails() {
        assertEquals(
            "CF_UPLOAD_STORE_FILE must be absolute: upload.p12",
            failure(values + ("CF_UPLOAD_STORE_FILE" to "upload.p12")),
        )
    }
}
