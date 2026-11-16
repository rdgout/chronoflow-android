package net.chronoflow.core.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.security.MessageDigest

class FontHashTest {
    private val fontDir = File("src/main/res/font")

    private val expected =
        mapOf(
            "figtree_400.ttf" to "686c2512dfe40452a46d04008cd4969810f8a3ff3f4e5a8a627b3260aa053344",
            "figtree_500.ttf" to "1cafc3d5c8d2b81d2a4f4a88958445f9169e3bfe669122c1226ad4fec770e113",
            "figtree_600.ttf" to "8fb8e0dd3c66a26f0561b626f61aa88eba919da9e596b0b1e8959e86951d4555",
            "figtree_700.ttf" to "e3618108cf5b2ed4f5af6c0848e2a48bbda4eea33caa2cb34650c86701c2ab1c",
            "fraunces_400_opsz20.ttf" to "1d5e40673d093bb6047b60f0b4618fb9b93055ac4dad2f9dfc07a7d90535f3e5",
            "fraunces_400_opsz32.ttf" to "e5614ccbec229d798127395a494514bc5f458c3da283d0d25e60e4326da2bb1d",
            "fraunces_400_opsz48.ttf" to "c9bdb5b1d9e24187e830b50598ef07ee19d75da8a34ab43867340f4199b4b9f2",
            "fraunces_500_opsz20.ttf" to "fc7a3d5221232b1c5e35562776d6d6324b128e2268b43e9c2a8ea04e56c0f5b1",
            "fraunces_500_opsz32.ttf" to "3ddf293fb40258e762df17f88106667ceab83cb1944ede7e4e1dcd783862694f",
            "fraunces_500_opsz48.ttf" to "6c67072103e1b91abbebe36c057292ab195c403271cb87028c60c11eaa00379d",
        )

    private fun sha256(bytes: ByteArray) =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") {
            "%02x".format(it)
        }

    private fun tables(bytes: ByteArray): Map<String, Int> {
        val buffer = ByteBuffer.wrap(bytes)
        val count = buffer.getShort(4).toInt() and 0xFFFF
        return (0 until count).associate { index ->
            val record = 12 + index * 16
            String(bytes, record, 4, Charsets.US_ASCII) to buffer.getInt(record + 8)
        }
    }

    @Test
    fun exactlyTheTenFontsExist() {
        assertEquals(
            expected.keys,
            fontDir
                .listFiles()
                .orEmpty()
                .map { it.name }
                .toSet(),
        )
    }

    @Test
    fun everyFontIsByteIdenticalToIOS() {
        for ((name, hash) in expected) assertEquals(name, hash, sha256(File(fontDir, name).readBytes()))
    }

    @Test
    fun everyFontIsStatic() {
        for (name in expected.keys) assertFalse("$name has fvar", "fvar" in tables(File(fontDir, name).readBytes()))
    }

    @Test
    fun weightClassMatchesTheFileName() {
        for (name in expected.keys) {
            val bytes = File(fontDir, name).readBytes()
            val os2 = tables(bytes).getValue("OS/2")
            val weight = ByteBuffer.wrap(bytes).getShort(os2 + 4).toInt() and 0xFFFF
            assertEquals(
                name,
                name
                    .substringAfter('_')
                    .substringBefore('_')
                    .substringBefore('.')
                    .toInt(),
                weight,
            )
        }
    }

    @Test
    fun bothLicencesAreShipped() {
        for (name in listOf("Figtree-OFL.txt", "Fraunces-OFL.txt")) {
            assertTrue(name, File("src/main/assets/licenses/$name").readText().contains("SIL OPEN FONT LICENSE"))
        }
    }
}
