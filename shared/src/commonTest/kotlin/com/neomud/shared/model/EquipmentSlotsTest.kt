package com.neomud.shared.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EquipmentSlotsTest {

    @Test
    fun testDefaultSlotsContainsAllTenSlots() {
        assertEquals(10, EquipmentSlots.DEFAULT_SLOTS.size)
    }

    @Test
    fun testDefaultSlotsContainsNeck() {
        assertTrue(EquipmentSlots.NECK in EquipmentSlots.DEFAULT_SLOTS, "DEFAULT_SLOTS should contain neck")
    }

    @Test
    fun testDefaultSlotsContainsRing() {
        assertTrue(EquipmentSlots.RING in EquipmentSlots.DEFAULT_SLOTS, "DEFAULT_SLOTS should contain ring")
    }

    @Test
    fun testNeckConstantValue() {
        assertEquals("neck", EquipmentSlots.NECK)
    }

    @Test
    fun testRingConstantValue() {
        assertEquals("ring", EquipmentSlots.RING)
    }

    @Test
    fun testDefaultSlotsContainsBack() {
        assertTrue(EquipmentSlots.BACK in EquipmentSlots.DEFAULT_SLOTS, "DEFAULT_SLOTS should contain back")
    }

    @Test
    fun testBackConstantValue() {
        assertEquals("back", EquipmentSlots.BACK)
    }

    @Test
    fun testDefaultSlotsContainsAllOriginalSlots() {
        val expected = setOf("head", "neck", "chest", "legs", "feet", "hands", "ring", "back", "weapon", "shield")
        assertEquals(expected, EquipmentSlots.DEFAULT_SLOTS.toSet())
    }
}
