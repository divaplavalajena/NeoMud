package com.neomud.server.world

import com.neomud.server.defaultWorldSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ItemCatalogTest {

    private fun load() = ItemCatalog.load(defaultWorldSource())

    @Test
    fun testLoadItemsFromJson() {
        val catalog = load()
        assertTrue(catalog.itemCount >= 38, "Should load at least 38 items")
    }

    @Test
    fun testLookupWeapon() {
        val catalog = load()
        val sword = catalog.getItem("item:iron_sword")
        assertNotNull(sword)
        assertEquals("Iron Sword", sword.name)
        assertEquals("weapon", sword.type)
        assertEquals("weapon", sword.slot)
        assertEquals(8, sword.damageBonus)
        assertEquals(8, sword.damageRange)
    }

    @Test
    fun testLookupArmor() {
        val catalog = load()
        val chest = catalog.getItem("item:leather_chest")
        assertNotNull(chest)
        assertEquals("Leather Vest", chest.name)
        assertEquals("armor", chest.type)
        assertEquals("chest", chest.slot)
        assertEquals(3, chest.armorValue)
    }

    @Test
    fun testLookupConsumable() {
        val catalog = load()
        val potion = catalog.getItem("item:health_potion")
        assertNotNull(potion)
        assertEquals("Health Potion", potion.name)
        assertEquals("consumable", potion.type)
        assertTrue(potion.stackable)
        assertEquals("heal:25", potion.useEffect)
    }

    @Test
    fun testUnknownItemReturnsNull() {
        val catalog = load()
        assertNull(catalog.getItem("item:nonexistent"))
    }

    @Test
    fun testLookupAccessory() {
        val catalog = load()
        val amulet = catalog.getItem("item:amulet_of_warding")
        assertNotNull(amulet)
        assertEquals("Amulet of Warding", amulet.name)
        assertEquals("armor", amulet.type)
        assertEquals("neck", amulet.slot)
        assertEquals(3, amulet.armorValue)
        assertEquals(3, amulet.levelRequirement)

        val ring = catalog.getItem("item:ring_of_intellect")
        assertNotNull(ring)
        assertEquals("Ring of Intellect", ring.name)
        assertEquals("ring", ring.slot)
        assertEquals(2, ring.damageBonus)
    }

    @Test
    fun testLookupMagicWeapon() {
        val catalog = load()
        val staff = catalog.getItem("item:mystic_staff")
        assertNotNull(staff)
        assertEquals("Mystic Staff", staff.name)
        assertEquals("weapon", staff.type)
        assertEquals(10, staff.damageBonus)
        assertEquals(10, staff.damageRange)
        assertEquals(5, staff.levelRequirement)
    }

    @Test
    fun testLookupGreaterPotions() {
        val catalog = load()
        val hp = catalog.getItem("item:greater_health_potion")
        assertNotNull(hp)
        assertEquals("consumable", hp.type)
        assertEquals("heal:60", hp.useEffect)
        assertTrue(hp.stackable)

        val mp = catalog.getItem("item:greater_mana_potion")
        assertNotNull(mp)
        assertEquals("consumable", mp.type)
        assertEquals("mana:50", mp.useEffect)
    }

    @Test
    fun testLookupScrolls() {
        val catalog = load()
        val fireball = catalog.getItem("item:scroll_of_fireball")
        assertNotNull(fireball)
        assertEquals("consumable", fireball.type)
        assertEquals("damage:35", fireball.useEffect)
        assertTrue(fireball.stackable)

        val healing = catalog.getItem("item:scroll_of_healing")
        assertNotNull(healing)
        assertEquals("heal:40", healing.useEffect)
    }

    @Test
    fun testLookupTavernItems() {
        val catalog = load()
        val ale = catalog.getItem("item:ale")
        assertNotNull(ale)
        assertEquals("consumable", ale.type)
        assertEquals("heal:10", ale.useEffect)
        assertTrue(ale.stackable)

        val bread = catalog.getItem("item:bread_loaf")
        assertNotNull(bread)
        assertEquals("heal:15", bread.useEffect)
    }

    @Test
    fun testLookupEnchantedArmor() {
        val catalog = load()
        val robes = catalog.getItem("item:enchanted_robes")
        assertNotNull(robes)
        assertEquals("armor", robes.type)
        assertEquals("chest", robes.slot)
        assertEquals(4, robes.armorValue)
        assertEquals(2, robes.damageBonus)
        assertEquals(5, robes.levelRequirement)
    }

    @Test
    fun testLookupCraftingMaterials() {
        val catalog = load()

        val hide = catalog.getItem("item:marsh_hide")
        assertNotNull(hide)
        assertEquals("Marsh Hide", hide.name)
        assertEquals("crafting", hide.type)
        assertEquals(12, hide.value)
        assertTrue(hide.stackable)
        assertEquals(20, hide.maxStack)

        val essence = catalog.getItem("item:wraith_essence")
        assertNotNull(essence)
        assertEquals("Wraith Essence", essence.name)
        assertEquals("crafting", essence.type)
        assertEquals(18, essence.value)

        val shard = catalog.getItem("item:obsidian_shard")
        assertNotNull(shard)
        assertEquals("Obsidian Shard", shard.name)
        assertEquals("crafting", shard.type)
        assertEquals(25, shard.value)
    }

    @Test
    fun testLookupCraftedConsumable() {
        val catalog = load()
        val vial = catalog.getItem("item:antivenom_vial")
        assertNotNull(vial)
        assertEquals("Antivenom Vial", vial.name)
        assertEquals("consumable", vial.type)
        assertEquals("cure_dot", vial.useEffect)
        assertTrue(vial.stackable)

        val tonic = catalog.getItem("item:fortifying_tonic")
        assertNotNull(tonic)
        assertEquals("buff_max_hp:20:20", tonic.useEffect)

        val draught = catalog.getItem("item:wraith_draught")
        assertNotNull(draught)
        assertEquals("buff_damage:3:20", draught.useEffect)
    }

    @Test
    fun testLookupCraftedWeapon() {
        val catalog = load()
        val edge = catalog.getItem("item:obsidian_edge")
        assertNotNull(edge)
        assertEquals("weapon", edge.type)
        assertEquals("weapon", edge.slot)
        assertEquals(14, edge.damageBonus)
        assertEquals(11, edge.damageRange)
        assertEquals(6, edge.levelRequirement)
    }

    @Test
    fun testLookupCraftedArmor() {
        val catalog = load()
        val cloak = catalog.getItem("item:wolf_pelt_cloak")
        assertNotNull(cloak)
        assertEquals("armor", cloak.type)
        assertEquals("back", cloak.slot)
        assertEquals(2, cloak.armorValue)
    }

    @Test
    fun testLookupCraftedScroll() {
        val catalog = load()
        val venom = catalog.getItem("item:scroll_of_venom")
        assertNotNull(venom)
        assertEquals("consumable", venom.type)
        assertEquals("damage:40", venom.useEffect)
    }

    @Test
    fun testGetAllItems() {
        val catalog = load()
        val all = catalog.getAllItems()
        assertTrue(all.size >= 58, "Should have at least 58 items (38 base + 20 crafted), got ${all.size}")
        val ids = all.map { it.id }.toSet()
        assertTrue("item:iron_sword" in ids)
        assertTrue("item:health_potion" in ids)
        assertTrue("item:wolf_pelt" in ids)
        assertTrue("item:mystic_staff" in ids)
        assertTrue("item:amulet_of_warding" in ids)
        assertTrue("item:scroll_of_fireball" in ids)
        assertTrue("item:antivenom_vial" in ids)
        assertTrue("item:obsidian_edge" in ids)
        assertTrue("item:wolf_pelt_cloak" in ids)
    }
}
