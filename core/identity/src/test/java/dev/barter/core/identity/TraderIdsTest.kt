package dev.barter.core.identity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TraderIdsTest {

    private val secret = "device-secret".encodeToByteArray()
    private val otherSecret = "other-device-secret".encodeToByteArray()

    @Test
    fun `the same number and secret always give the same id`() {
        val first = TraderIds.fromPhoneNumber("+15551234567", secret)

        assertEquals(first, TraderIds.fromPhoneNumber("+15551234567", secret))
    }

    @Test
    fun `the id does not contain the phone number`() {
        val id = TraderIds.fromPhoneNumber("+15551234567", secret)

        assertEquals(TraderIds.ID_HEX_LENGTH, id.hex.length)
        assertNotEquals(id.hex, "15551234567")
    }

    @Test
    fun `formatting differences in the same number give the same id`() {
        val canonical = TraderIds.fromPhoneNumber("+15551234567", secret)

        listOf("+1 555 123 4567", "+1-555-123-4567", "+1 (555) 123-4567", "15551234567").forEach { variant ->
            assertEquals(variant, canonical, TraderIds.fromPhoneNumber(variant, secret))
        }
    }

    @Test
    fun `different numbers give unrelated ids`() {
        val me = TraderIds.fromPhoneNumber("+15551234567", secret)

        assertNotEquals(me, TraderIds.fromPhoneNumber("+15557654321", secret))
    }

    @Test
    fun `the device secret changes the id even for the same number`() {
        val mine = TraderIds.fromPhoneNumber("+15551234567", secret)

        assertNotEquals(mine, TraderIds.fromPhoneNumber("+15551234567", otherSecret))
    }

    @Test
    fun `the local fallback id is stable`() {
        assertEquals(TraderIds.local(), TraderIds.local())
    }
}
