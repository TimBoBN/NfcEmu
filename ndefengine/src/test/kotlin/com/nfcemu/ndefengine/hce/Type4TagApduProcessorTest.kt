package com.nfcemu.ndefengine.hce

import com.nfcemu.ndefengine.NdefMessageFactory
import com.nfcemu.ndefengine.NdefPayload
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Type4TagApduProcessorTest {

    private val ndefAid = byteArrayOf(0xD2.toByte(), 0x76, 0x00, 0x00, 0x85.toByte(), 0x01, 0x01)
    private val ccFileId = byteArrayOf(0xE1.toByte(), 0x03)
    private val ndefFileId = byteArrayOf(0xE1.toByte(), 0x04)

    private fun selectApdu(p1: Int, p2: Int, data: ByteArray, le: Int? = null): ByteArray {
        val header = byteArrayOf(0x00, 0xA4.toByte(), p1.toByte(), p2.toByte(), data.size.toByte())
        return header + data + (le?.let { byteArrayOf(it.toByte()) } ?: ByteArray(0))
    }

    private fun selectAid() = selectApdu(0x04, 0x00, ndefAid)
    private fun selectCc() = selectApdu(0x00, 0x0C, ccFileId)
    private fun selectNdef() = selectApdu(0x00, 0x0C, ndefFileId)

    private fun readBinary(offset: Int, le: Int): ByteArray =
        byteArrayOf(0x00, 0xB0.toByte(), (offset shr 8).toByte(), (offset and 0xFF).toByte(), le.toByte())

    private fun sw(response: ByteArray): Pair<Int, Int> {
        val a = response[response.size - 2].toInt() and 0xFF
        val b = response[response.size - 1].toInt() and 0xFF
        return a to b
    }

    @Test
    fun `select correct aid returns 9000`() {
        val processor = Type4TagApduProcessor()
        assertEquals(0x90 to 0x00, sw(processor.process(selectAid())))
    }

    @Test
    fun `select unknown aid returns file-not-found`() {
        val processor = Type4TagApduProcessor()
        val badAid = byteArrayOf(0x01, 0x02, 0x03)
        assertEquals(0x6A to 0x82, sw(processor.process(selectApdu(0x04, 0x00, badAid))))
    }

    @Test
    fun `select cc file then read binary returns capability container plus 9000`() {
        val processor = Type4TagApduProcessor()
        processor.process(selectAid())
        assertEquals(0x90 to 0x00, sw(processor.process(selectCc())))

        val response = processor.process(readBinary(0, 0xFF))
        assertEquals(0x90 to 0x00, sw(response))
        val ccBytes = response.copyOfRange(0, response.size - 2)
        assertEquals(15, ccBytes.size)
        assertEquals(0xE1, ccBytes[9].toInt() and 0xFF, "CC must reference NDEF file id E104")
    }

    @Test
    fun `select unknown file id returns file-not-found`() {
        val processor = Type4TagApduProcessor()
        processor.process(selectAid())
        val response = processor.process(selectApdu(0x00, 0x0C, byteArrayOf(0x12, 0x34)))
        assertEquals(0x6A to 0x82, sw(response))
    }

    @Test
    fun `select with unsupported p1 p2 returns wrong-parameters`() {
        val processor = Type4TagApduProcessor()
        val response = processor.process(selectApdu(0x01, 0x02, ndefAid))
        assertEquals(0x6A to 0x86, sw(response))
    }

    @Test
    fun `read binary without prior select returns file-not-found`() {
        val processor = Type4TagApduProcessor()
        assertEquals(0x6A to 0x82, sw(processor.process(readBinary(0, 0xFF))))
    }

    @Test
    fun `read binary offset beyond file length returns wrong-parameters`() {
        val processor = Type4TagApduProcessor()
        processor.process(selectAid())
        processor.process(selectCc())
        val response = processor.process(readBinary(9999, 0xFF))
        assertEquals(0x6A to 0x86, sw(response))
    }

    @Test
    fun `read binary exactly at end of file returns empty data with success`() {
        val processor = Type4TagApduProcessor()
        processor.process(selectAid())
        processor.process(selectCc())
        val response = processor.process(readBinary(15, 0xFF))
        assertEquals(0x90 to 0x00, sw(response))
        assertEquals(2, response.size, "only the status word, no data")
    }

    @Test
    fun `le of zero is treated as 256 per iso7816-4 and does not crash`() {
        val processor = Type4TagApduProcessor()
        processor.process(selectAid())
        processor.process(selectCc())
        val response = processor.process(readBinary(0, 0x00))
        assertEquals(0x90 to 0x00, sw(response))
        assertEquals(15 + 2, response.size, "whole 15-byte CC fits within Le=256")
    }

    @Test
    fun `read binary with short-ef-identifier bit set is rejected`() {
        val processor = Type4TagApduProcessor()
        processor.process(selectAid())
        processor.process(selectCc())
        val apdu = byteArrayOf(0x00, 0xB0.toByte(), 0x80.toByte(), 0x00, 0x10)
        assertEquals(0x6A to 0x86, sw(processor.process(apdu)))
    }

    @Test
    fun `unknown instruction returns fallback status word without crashing`() {
        val processor = Type4TagApduProcessor()
        val apdu = byteArrayOf(0x00, 0xD6.toByte(), 0x00, 0x00, 0x00) // UPDATE BINARY, unsupported
        assertEquals(0x6F to 0x00, sw(processor.process(apdu)))
    }

    @Test
    fun `unsupported class byte returns fallback status word`() {
        val processor = Type4TagApduProcessor()
        val apdu = byteArrayOf(0x80.toByte(), 0xB0.toByte(), 0x00, 0x00, 0x00)
        assertEquals(0x6F to 0x00, sw(processor.process(apdu)))
    }

    @Test
    fun `null apdu is rejected with wrong-length rather than crashing`() {
        val processor = Type4TagApduProcessor()
        assertEquals(0x67 to 0x00, sw(processor.process(null)))
    }

    @Test
    fun `truncated apdu shorter than 4 bytes is rejected with wrong-length`() {
        val processor = Type4TagApduProcessor()
        assertEquals(0x67 to 0x00, sw(processor.process(byteArrayOf(0x00, 0xA4.toByte()))))
    }

    @Test
    fun `select apdu whose declared lc exceeds actual remaining bytes is rejected`() {
        val processor = Type4TagApduProcessor()
        // Lc says 7 bytes follow but only 2 are actually present.
        val malformed = byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, 0x07, 0x01, 0x02)
        assertEquals(0x67 to 0x00, sw(processor.process(malformed)))
    }

    @Test
    fun `read binary chunking across multiple calls with small le reconstructs the full ndef file`() {
        val processor = Type4TagApduProcessor()
        val payload = NdefPayload.VCard(name = "Chunking Test", address = "X".repeat(500))
        val ndefBytes = NdefMessageFactory.build(payload)
        processor.updateNdefMessage(ndefBytes)

        processor.process(selectAid())
        processor.process(selectNdef())

        val chunkSize = 40
        val fullFile = ByteArray(2 + ndefBytes.size)
        var offset = 0
        var loops = 0
        while (offset < fullFile.size) {
            val response = processor.process(readBinary(offset, chunkSize))
            val statusWord = sw(response)
            assertEquals(0x90 to 0x00, statusWord)
            val data = response.copyOfRange(0, response.size - 2)
            assertTrue(data.isNotEmpty())
            data.copyInto(fullFile, offset)
            offset += data.size
            loops++
            check(loops < 1000) { "chunking loop did not terminate" }
        }

        val nlen = ((fullFile[0].toInt() and 0xFF) shl 8) or (fullFile[1].toInt() and 0xFF)
        assertEquals(ndefBytes.size, nlen)
        val reconstructed = fullFile.copyOfRange(2, 2 + nlen)
        assertTrue(reconstructed.contentEquals(ndefBytes))
        assertTrue(loops > 1, "chunk size was deliberately smaller than the file, must take multiple reads")
    }

    @Test
    fun `updating the ndef message resets file selection so a fresh select is required`() {
        val processor = Type4TagApduProcessor()
        processor.process(selectAid())
        processor.process(selectNdef())

        processor.updateNdefMessage(NdefMessageFactory.build(NdefPayload.Text("new profile")))

        val response = processor.process(readBinary(0, 0xFF))
        assertEquals(0x6A to 0x82, sw(response), "selection must be invalidated after a profile switch")
    }

    @Test
    fun `updated ndef message is reflected in subsequent reads`() {
        val processor = Type4TagApduProcessor()
        val first = NdefMessageFactory.build(NdefPayload.Text("first"))
        val second = NdefMessageFactory.build(NdefPayload.Text("second-profile-content"))
        processor.updateNdefMessage(first)

        processor.process(selectAid())
        processor.process(selectNdef())
        val firstRead = processor.process(readBinary(0, 0xFF))
        val firstData = firstRead.copyOfRange(2, firstRead.size - 2) // skip NLEN
        assertTrue(String(firstData, Charsets.UTF_8).contains("first"))

        processor.updateNdefMessage(second)
        processor.process(selectAid())
        processor.process(selectNdef())
        val secondRead = processor.process(readBinary(0, 0xFF))
        val secondData = secondRead.copyOfRange(2, secondRead.size - 2)
        assertTrue(String(secondData, Charsets.UTF_8).contains("second-profile-content"))
    }
}
