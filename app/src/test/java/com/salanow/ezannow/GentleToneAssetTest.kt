package com.salanow.ezannow

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class GentleToneAssetTest {

    private val expectedMp3Files = listOf(
        "ezannow_melody_soft.mp3",
        "ezannow_melody_calm.mp3",
        "ezannow_melody_short.mp3",
        "ezannow_ney_classic.mp3",
        "ezannow_ney_peaceful.mp3",
        "ezannow_kanun_classic.mp3",
        "ezannow_tasavvuf_melody.mp3",
        "ezannow_adhan_short_1.mp3",
        "ezannow_adhan_short_2.mp3",
        "ezannow_fajr_call.mp3",
        "ezannow_alarm_classic.mp3"
    )

    @Test
    fun allElevenNotificationMp3sExistHaveCleanMpegHeadersAndAreDistinct() {
        val seenHashes = mutableSetOf<String>()
        val md = MessageDigest.getInstance("SHA-256")

        for (fileName in expectedMp3Files) {
            val candidatePaths = listOf(
                File("src/main/res/raw/$fileName"),
                File("app/src/main/res/raw/$fileName")
            )
            val mp3File = candidatePaths.firstOrNull { it.exists() }
                ?: error("$fileName not found in ${candidatePaths.map { it.absolutePath }}")

            val bytes = mp3File.readBytes()
            assertTrue("$fileName unexpectedly small: ${bytes.size}", bytes.size > 45_000)

            val hasId3Header = bytes.size >= 3 &&
                bytes[0] == 'I'.code.toByte() &&
                bytes[1] == 'D'.code.toByte() &&
                bytes[2] == '3'.code.toByte()
            assertFalse("$fileName should not contain leading ID3 metadata", hasId3Header)

            val hasMpegFrameSync = bytes.size >= 2 &&
                (bytes[0].toInt() and 0xFF) == 0xFF &&
                (bytes[1].toInt() and 0xE0) == 0xE0
            assertTrue("$fileName missing valid MPEG sync header", hasMpegFrameSync)

            val hashHex = md.digest(bytes).joinToString("") { "%02x".format(it) }
            assertTrue("$fileName duplicated another sound", seenHashes.add(hashHex))
        }
    }
}
