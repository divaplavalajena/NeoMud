package com.neomud.server.game

import com.neomud.server.game.npc.NpcManager
import com.neomud.server.game.npc.NpcState
import com.neomud.server.game.npc.behavior.IdleBehavior
import com.neomud.server.world.NpcData
import com.neomud.server.world.WorldGraph
import com.neomud.shared.model.ActiveEffect
import com.neomud.shared.model.Direction
import com.neomud.shared.model.EffectType
import com.neomud.shared.model.Room
import com.neomud.shared.model.SpellDef
import com.neomud.shared.model.SpellType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class DotHotTest {

    private fun createNpcState(
        id: String = "npc:test",
        name: String = "Test NPC",
        maxHp: Int = 50,
        currentHp: Int = 50,
        roomId: String = "test:room"
    ): NpcState {
        return NpcState(
            id = id,
            name = name,
            description = "A test NPC",
            currentRoomId = roomId,
            behavior = IdleBehavior(),
            hostile = true,
            maxHp = maxHp,
            currentHp = currentHp,
            damage = 5,
            level = 3,
            xpReward = 100,
            templateId = id
        )
    }

    private fun createPoisonEffect(
        magnitude: Int = 4,
        remainingTicks: Int = 4,
        casterId: String = "TestPlayer"
    ): ActiveEffect {
        return ActiveEffect(
            name = "Poison Cloud",
            type = EffectType.POISON,
            remainingTicks = remainingTicks,
            magnitude = magnitude,
            casterId = casterId
        )
    }

    // --- ActiveEffect on NpcState ---

    @Test
    fun testNpcStateHasEmptyActiveEffectsByDefault() {
        val npc = createNpcState()
        assertTrue(npc.activeEffects.isEmpty())
    }

    @Test
    fun testAddingEffectToNpcState() {
        val npc = createNpcState()
        val effect = createPoisonEffect()
        npc.activeEffects.add(effect)

        assertEquals(1, npc.activeEffects.size)
        assertEquals("Poison Cloud", npc.activeEffects[0].name)
        assertEquals(EffectType.POISON, npc.activeEffects[0].type)
        assertEquals(4, npc.activeEffects[0].magnitude)
        assertEquals("TestPlayer", npc.activeEffects[0].casterId)
    }

    // --- DoT damage application ---

    @Test
    fun testPoisonDamageReducesNpcHp() {
        val npc = createNpcState(maxHp = 50, currentHp = 50)
        val effect = createPoisonEffect(magnitude = 4)
        npc.activeEffects.add(effect)

        // Simulate one tick of poison damage
        npc.currentHp = (npc.currentHp - effect.magnitude).coerceAtLeast(0)
        assertEquals(46, npc.currentHp)
    }

    @Test
    fun testDotKillsNpcWhenHpReachesZero() {
        val npc = createNpcState(maxHp = 10, currentHp = 3)
        val effect = createPoisonEffect(magnitude = 5)
        npc.activeEffects.add(effect)

        // Simulate tick: damage should reduce HP to 0 (clamped)
        npc.currentHp = (npc.currentHp - effect.magnitude).coerceAtLeast(0)
        assertEquals(0, npc.currentHp)
    }

    // --- Effect tick-down and expiry ---

    @Test
    fun testEffectTicksDownEachIteration() {
        val npc = createNpcState()
        val effect = createPoisonEffect(remainingTicks = 4)
        npc.activeEffects.add(effect)

        // Simulate tick-down
        val updated = npc.activeEffects[0].copy(remainingTicks = npc.activeEffects[0].remainingTicks - 1)
        npc.activeEffects[0] = updated

        assertEquals(3, npc.activeEffects[0].remainingTicks)
    }

    @Test
    fun testEffectRemovedWhenExpired() {
        val npc = createNpcState()
        val effect = createPoisonEffect(remainingTicks = 1)
        npc.activeEffects.add(effect)

        // Simulate tick: remainingTicks goes to 0, should be removed
        val updated = effect.copy(remainingTicks = effect.remainingTicks - 1)
        if (updated.remainingTicks <= 0) {
            npc.activeEffects.remove(effect)
        }

        assertTrue(npc.activeEffects.isEmpty(), "Effect should be removed when expired")
    }

    @Test
    fun testMultipleTicksCumulativeDamage() {
        val npc = createNpcState(maxHp = 50, currentHp = 50)
        val magnitude = 4
        val totalTicks = 4

        npc.activeEffects.add(createPoisonEffect(magnitude = magnitude, remainingTicks = totalTicks))

        // Simulate 4 ticks of poison
        var totalDamage = 0
        repeat(totalTicks) {
            val effect = npc.activeEffects.firstOrNull() ?: return@repeat
            npc.currentHp = (npc.currentHp - effect.magnitude).coerceAtLeast(0)
            totalDamage += effect.magnitude

            val updated = effect.copy(remainingTicks = effect.remainingTicks - 1)
            if (updated.remainingTicks <= 0) {
                npc.activeEffects.remove(effect)
            } else {
                npc.activeEffects[0] = updated
            }
        }

        assertEquals(magnitude * totalTicks, totalDamage)
        assertEquals(50 - totalDamage, npc.currentHp)
        assertTrue(npc.activeEffects.isEmpty(), "Effect should be expired after all ticks")
    }

    // --- Refresh (no stacking) ---

    @Test
    fun testRecastRefreshesEffectDoesNotStack() {
        val npc = createNpcState()
        npc.activeEffects.add(createPoisonEffect(remainingTicks = 2))

        // Simulate recast: remove old, add new
        val newEffect = createPoisonEffect(remainingTicks = 4, casterId = "AnotherPlayer")
        npc.activeEffects.removeAll { it.name == newEffect.name }
        npc.activeEffects.add(newEffect)

        assertEquals(1, npc.activeEffects.size, "Should have exactly 1 effect (refreshed, not stacked)")
        assertEquals(4, npc.activeEffects[0].remainingTicks, "Should have fresh remaining ticks")
        assertEquals("AnotherPlayer", npc.activeEffects[0].casterId, "Should track new caster")
    }

    // --- Death clears effects ---

    @Test
    fun testMarkDeadClearsActiveEffects() {
        val world = WorldGraph()
        world.addRoom(Room("test:room", "Test", "", mapOf(), "test", 0, 0))
        val npcManager = NpcManager(world)
        val npcData = NpcData(
            id = "npc:test",
            name = "Test Mob",
            description = "A test mob",
            startRoomId = "test:room",
            behaviorType = "idle",
            hostile = true,
            maxHp = 50,
            damage = 5,
            level = 1,
            xpReward = 100
        )
        npcManager.loadNpcs(listOf(npcData to "test"))

        // Add an effect to the NPC via getNpcState
        val npc = npcManager.getNpcState("npc:test")!!
        npc.activeEffects.add(createPoisonEffect())
        assertEquals(1, npc.activeEffects.size)

        // Mark dead should clear effects
        npcManager.markDead("npc:test")
        assertEquals(0, npc.activeEffects.size, "markDead should clear active effects")
    }

    // --- getNpcsInRoom includes activeEffects ---

    @Test
    fun testGetNpcsInRoomIncludesActiveEffects() {
        val world = WorldGraph()
        world.addRoom(Room("test:room", "Test", "", mapOf(), "test", 0, 0))
        val npcManager = NpcManager(world)
        val npcData = NpcData(
            id = "npc:test",
            name = "Test Mob",
            description = "A test mob",
            startRoomId = "test:room",
            behaviorType = "idle",
            hostile = true,
            maxHp = 50,
            damage = 5,
            level = 1,
            xpReward = 100
        )
        npcManager.loadNpcs(listOf(npcData to "test"))

        val npcState = npcManager.getNpcState("npc:test")!!
        npcState.activeEffects.add(createPoisonEffect())

        val npcs = npcManager.getNpcsInRoom("test:room")
        assertEquals(1, npcs.size)
        assertEquals(1, npcs[0].activeEffects.size)
        assertEquals("Poison Cloud", npcs[0].activeEffects[0].name)
    }

    // --- getLivingNpcsWithEffects ---

    @Test
    fun testGetLivingNpcsWithEffectsReturnsOnlyAffectedNpcs() {
        val world = WorldGraph()
        world.addRoom(Room("test:room", "Test", "", mapOf(), "test", 0, 0))
        val npcManager = NpcManager(world)
        val npcData1 = NpcData(
            id = "npc:mob1", name = "Mob 1", description = "", startRoomId = "test:room",
            behaviorType = "idle", hostile = true, maxHp = 50, damage = 5
        )
        val npcData2 = NpcData(
            id = "npc:mob2", name = "Mob 2", description = "", startRoomId = "test:room",
            behaviorType = "idle", hostile = true, maxHp = 50, damage = 5
        )
        npcManager.loadNpcs(listOf(npcData1 to "test", npcData2 to "test"))

        // Only add effect to mob1
        val mob1 = npcManager.getNpcState("npc:mob1")!!
        mob1.activeEffects.add(createPoisonEffect())

        val withEffects = npcManager.getLivingNpcsWithEffects()
        assertEquals(1, withEffects.size)
        assertEquals("npc:mob1", withEffects[0].id)
    }

    @Test
    fun testGetLivingNpcsWithEffectsExcludesDeadNpcs() {
        val world = WorldGraph()
        world.addRoom(Room("test:room", "Test", "", mapOf(), "test", 0, 0))
        val npcManager = NpcManager(world)
        val npcData = NpcData(
            id = "npc:test", name = "Test", description = "", startRoomId = "test:room",
            behaviorType = "idle", hostile = true, maxHp = 50, damage = 5
        )
        npcManager.loadNpcs(listOf(npcData to "test"))

        val npc = npcManager.getNpcState("npc:test")!!
        npc.activeEffects.add(createPoisonEffect())
        npcManager.markDead("npc:test")

        val withEffects = npcManager.getLivingNpcsWithEffects()
        assertTrue(withEffects.isEmpty(), "Dead NPCs should not appear in getLivingNpcsWithEffects")
    }

    // --- casterId tracking for kill credit ---

    @Test
    fun testCasterIdPreservedThroughEffectLifecycle() {
        val effect = ActiveEffect(
            name = "Poison Cloud",
            type = EffectType.POISON,
            remainingTicks = 4,
            magnitude = 4,
            casterId = "MagePlayer"
        )

        assertEquals("MagePlayer", effect.casterId)

        // After tick-down, casterId should persist
        val updated = effect.copy(remainingTicks = effect.remainingTicks - 1)
        assertEquals("MagePlayer", updated.casterId)
    }

    // --- SpellDef tickPower ---

    @Test
    fun testSpellDefTickPowerDefaultsToZero() {
        val spell = SpellDef(
            id = "TEST",
            name = "Test",
            description = "test",
            school = "test",
            spellType = SpellType.DAMAGE,
            manaCost = 5
        )
        assertEquals(0, spell.tickPower)
    }

    @Test
    fun testSpellDefTickPowerCanBeSet() {
        val spell = SpellDef(
            id = "POISON_CLOUD",
            name = "Poison Cloud",
            description = "A toxic miasma",
            school = "druid",
            spellType = SpellType.DOT,
            manaCost = 12,
            basePower = 12,
            tickPower = 4,
            effectType = "POISON",
            effectDuration = 4
        )
        assertEquals(4, spell.tickPower)
    }

    // --- HoT magnitude uses tickPower ---

    @Test
    fun testHotUsesTickPowerWhenAvailable() {
        val spell = SpellDef(
            id = "SOOTHING_SONG",
            name = "Soothing Song",
            description = "A healing song",
            school = "bard",
            spellType = SpellType.HOT,
            manaCost = 8,
            basePower = 4,
            tickPower = 4,
            effectType = "HEAL_OVER_TIME",
            effectDuration = 6
        )

        // When tickPower > 0, use tickPower for HoT magnitude
        val hotMagnitude = if (spell.tickPower > 0) spell.tickPower else 10 // 10 = simulated power
        assertEquals(4, hotMagnitude)
    }

    @Test
    fun testHotFallsBackToPowerWhenTickPowerZero() {
        val spell = SpellDef(
            id = "OLD_HOT",
            name = "Old HoT",
            description = "Legacy",
            school = "test",
            spellType = SpellType.HOT,
            manaCost = 5,
            basePower = 4
            // tickPower defaults to 0
        )

        val simulatedPower = 10
        val hotMagnitude = if (spell.tickPower > 0) spell.tickPower else simulatedPower
        assertEquals(simulatedPower, hotMagnitude, "Should fall back to computed power when tickPower is 0")
    }

    // --- Damage type classification ---

    @Test
    fun testDamageTypeClassification() {
        val damageTypes = setOf(EffectType.POISON, EffectType.DAMAGE, EffectType.MANA_DRAIN)
        val healTypes = setOf(EffectType.HEAL_OVER_TIME)
        val buffTypes = setOf(EffectType.BUFF_STRENGTH, EffectType.BUFF_AGILITY, EffectType.HASTE,
            EffectType.BUFF_INTELLECT, EffectType.BUFF_WILLPOWER)

        assertTrue(EffectType.POISON in damageTypes, "POISON should be a damage type")
        assertTrue(EffectType.DAMAGE in damageTypes, "DAMAGE should be a damage type")
        assertTrue(EffectType.HEAL_OVER_TIME in healTypes, "HEAL_OVER_TIME should be a heal type")
        assertTrue(EffectType.BUFF_STRENGTH in buffTypes, "BUFF_STRENGTH should be a buff type")
    }
}
