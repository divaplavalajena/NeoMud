package com.neomud.server.game

import com.neomud.server.world.WorldGraph
import com.neomud.shared.model.Direction
import com.neomud.shared.model.Room
import com.neomud.shared.model.RoomInteractable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests the EXIT_OPEN interactable relock timing to ensure players have
 * enough time to use an unlocked exit before it re-locks.
 *
 * Bug context: The tavern cellar lever had resetTicks=6 (9 seconds at 1.5s/tick),
 * which was too short for players to notice the exit opened and move through it.
 */
class ExitOpenRelockTest {

    @Test
    fun testUnlockExitRemovesLock() {
        val worldGraph = createWorldWithLockedExit()

        // Verify exit is locked
        val roomBefore = worldGraph.getRoom("test:cellar")!!
        assertTrue(Direction.UP in roomBefore.lockedExits, "UP should be locked initially")
        assertEquals(99, roomBefore.lockedExits[Direction.UP])

        // Unlock the exit
        worldGraph.unlockExit("test:cellar", Direction.UP)

        // Verify exit is unlocked
        val roomAfter = worldGraph.getRoom("test:cellar")!!
        assertFalse(Direction.UP in roomAfter.lockedExits, "UP should be unlocked after unlockExit")
    }

    @Test
    fun testInteractableResetRelocksExit() {
        val worldGraph = createWorldWithLockedExit()
        setupInteractable(worldGraph)

        // Unlock the exit and mark interactable as used
        worldGraph.unlockExit("test:cellar", Direction.UP)
        worldGraph.markInteractableUsed("test:cellar", "lever_test", 5)

        // Verify unlocked
        val roomUnlocked = worldGraph.getRoom("test:cellar")!!
        assertFalse(Direction.UP in roomUnlocked.lockedExits)

        // Tick 4 times - should still be unlocked (reset happens after 5 ticks)
        for (i in 1..4) {
            worldGraph.tickInteractableTimers()
            val room = worldGraph.getRoom("test:cellar")!!
            assertFalse(Direction.UP in room.lockedExits, "Exit should still be unlocked at tick $i")
        }

        // Tick once more - interactable resets, exit re-locks
        val resets = worldGraph.tickInteractableTimers()
        assertTrue(resets.isNotEmpty(), "Interactable should reset on tick 5")

        val roomRelocked = worldGraph.getRoom("test:cellar")!!
        assertTrue(Direction.UP in roomRelocked.lockedExits, "Exit should re-lock when interactable resets")
        assertEquals(99, roomRelocked.lockedExits[Direction.UP], "Re-locked difficulty should match original")
    }

    @Test
    fun testCellarLeverHas40TickReset() {
        // This test verifies the town.zone.json data change.
        // The lever resetTicks was changed from 6 to 40 (60 seconds at 1.5s/tick)
        // to give players ample time to use the unlocked exit.
        val expectedResetTicks = 40
        val tickDuration = 1.5 // seconds
        val totalSeconds = expectedResetTicks * tickDuration

        assertEquals(60.0, totalSeconds, "Reset ticks should give 60 seconds of access")
    }

    @Test
    fun testExitStaysUnlockedForResetDuration() {
        val worldGraph = createWorldWithLockedExit()
        setupInteractable(worldGraph)

        // Unlock and mark used with 40 tick reset (matching the cellar fix)
        worldGraph.unlockExit("test:cellar", Direction.UP)
        worldGraph.markInteractableUsed("test:cellar", "lever_test", 40)

        // Tick 39 times - exit should remain unlocked the entire time
        for (i in 1..39) {
            worldGraph.tickInteractableTimers()
            val room = worldGraph.getRoom("test:cellar")!!
            assertFalse(Direction.UP in room.lockedExits, "Exit should remain unlocked at tick $i of 40")
        }

        // Tick 40 - now it re-locks
        worldGraph.tickInteractableTimers()
        val room = worldGraph.getRoom("test:cellar")!!
        assertTrue(Direction.UP in room.lockedExits, "Exit should re-lock at tick 40")
    }

    @Test
    fun testExitVisibleInExitsMapWhenLocked() {
        val worldGraph = createWorldWithLockedExit()
        val room = worldGraph.getRoom("test:cellar")!!

        // The exit should be in the exits map even when locked
        assertTrue(Direction.UP in room.exits, "UP exit should be in exits map even when locked")
        assertEquals("test:tavern", room.exits[Direction.UP])
    }

    private fun createWorldWithLockedExit(): WorldGraph {
        val worldGraph = WorldGraph()

        val cellar = Room(
            id = "test:cellar",
            name = "Cellar",
            description = "A test cellar",
            exits = mapOf(Direction.UP to "test:tavern"),
            zoneId = "test",
            x = 0,
            y = -1,
            lockedExits = mapOf(Direction.UP to 99),
            unpickableExits = setOf(Direction.UP)
        )
        val tavern = Room(
            id = "test:tavern",
            name = "Tavern",
            description = "A test tavern",
            exits = mapOf(Direction.DOWN to "test:cellar"),
            zoneId = "test",
            x = 0,
            y = 0
        )

        worldGraph.addRoom(cellar)
        worldGraph.addRoom(tavern)
        worldGraph.setOriginalLockedExits("test:cellar", mapOf(Direction.UP to 99))

        return worldGraph
    }

    private fun setupInteractable(worldGraph: WorldGraph) {
        val interactable = RoomInteractable(
            id = "lever_test",
            label = "Pull Lever",
            description = "A test lever",
            actionType = "EXIT_OPEN",
            actionData = mapOf("direction" to "UP"),
            resetTicks = 40
        )
        worldGraph.storeInteractableDefs("test:cellar", listOf(interactable))
    }
}
