package com.nfcemu.ndefengine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CapabilityContainerTest {

    @Test
    fun `cc file is always 15 bytes with fixed header fields`() {
        val cc = CapabilityContainer.build(42)
        assertEquals(15, cc.size)
        assertEquals(0x00, cc[0].toInt())
        assertEquals(0x0F, cc[1].toInt())
        assertEquals(0x20, cc[2].toInt())
        assertEquals(0x04, cc[7].toInt())
        assertEquals(0x06, cc[8].toInt())
        assertEquals(0xE1, cc[9].toInt() and 0xFF)
        assertEquals(0x04, cc[10].toInt())
        assertEquals(0x00, cc[13].toInt(), "read access must be granted")
        assertEquals(0xFF, cc[14].toInt() and 0xFF, "write access must be denied (read-only tag)")
    }

    @Test
    fun `max ndef file size field equals message length plus 2`() {
        val cc = CapabilityContainer.build(100)
        val maxSize = ((cc[11].toInt() and 0xFF) shl 8) or (cc[12].toInt() and 0xFF)
        assertEquals(102, maxSize)
    }

    @Test
    fun `zero length message is accepted`() {
        val cc = CapabilityContainer.build(0)
        val maxSize = ((cc[11].toInt() and 0xFF) shl 8) or (cc[12].toInt() and 0xFF)
        assertEquals(2, maxSize)
    }

    @Test
    fun `message length exceeding type 4 tag limits is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            CapabilityContainer.build(0xFFFE)
        }
    }

    @Test
    fun `negative message length is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            CapabilityContainer.build(-1)
        }
    }
}
