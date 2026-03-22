package com.neomud.server

import com.neomud.server.persistence.repository.DiscoveryRepository
import com.neomud.server.persistence.repository.PlayerDiscoveryData
import com.neomud.server.persistence.tables.PlayerDiscoveryTable
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiscoveryRepositoryTest {

    private fun withTestDb(block: () -> Unit) {
        val tmpFile = File.createTempFile("neomud_discovery_test_", ".db")
        tmpFile.deleteOnExit()
        tmpFile.delete()
        Database.connect("jdbc:sqlite:${tmpFile.absolutePath}", driver = "org.sqlite.JDBC")
        transaction { SchemaUtils.create(PlayerDiscoveryTable) }
        block()
    }

    @Test
    fun testLoadEmptyDiscovery() = withTestDb {
        val repo = DiscoveryRepository()
        val data = repo.loadPlayerDiscovery("nobody")
        assertTrue(data.visitedRooms.isEmpty())
        assertTrue(data.discoveredHiddenExits.isEmpty())
        assertTrue(data.discoveredLockedExits.isEmpty())
        assertTrue(data.discoveredInteractables.isEmpty())
        assertTrue(data.tutorials.isEmpty())
    }

    @Test
    fun testSaveAndLoadVisitedRooms() = withTestDb {
        val repo = DiscoveryRepository()
        val data = PlayerDiscoveryData(
            visitedRooms = setOf("town:square", "town:gate", "forest:path"),
            discoveredHiddenExits = emptySet(),
            discoveredLockedExits = emptySet(),
            discoveredInteractables = emptySet()
        )
        repo.savePlayerDiscovery("bob", data)
        val loaded = repo.loadPlayerDiscovery("bob")
        assertEquals(setOf("town:square", "town:gate", "forest:path"), loaded.visitedRooms)
    }

    @Test
    fun testSaveAndLoadAllTypes() = withTestDb {
        val repo = DiscoveryRepository()
        val data = PlayerDiscoveryData(
            visitedRooms = setOf("town:square", "town:gate"),
            discoveredHiddenExits = setOf("town:square:EAST", "forest:path:NORTH"),
            discoveredLockedExits = setOf("town:gate:NORTH"),
            discoveredInteractables = setOf("town:square::lever_1", "dungeon:entry::chest_2"),
            tutorials = setOf("welcome", "combat_intro")
        )
        repo.savePlayerDiscovery("alice", data)
        val loaded = repo.loadPlayerDiscovery("alice")
        assertEquals(data.visitedRooms, loaded.visitedRooms)
        assertEquals(data.discoveredHiddenExits, loaded.discoveredHiddenExits)
        assertEquals(data.discoveredLockedExits, loaded.discoveredLockedExits)
        assertEquals(data.discoveredInteractables, loaded.discoveredInteractables)
        assertEquals(data.tutorials, loaded.tutorials)
    }

    @Test
    fun testSaveIsIdempotent() = withTestDb {
        val repo = DiscoveryRepository()
        val data = PlayerDiscoveryData(
            visitedRooms = setOf("town:square"),
            discoveredHiddenExits = emptySet(),
            discoveredLockedExits = emptySet(),
            discoveredInteractables = emptySet(),
            tutorials = setOf("welcome")
        )
        repo.savePlayerDiscovery("bob", data)
        // Save again with same + new data — should not fail or duplicate
        val updated = data.copy(
            visitedRooms = setOf("town:square", "town:gate"),
            tutorials = setOf("welcome", "combat_intro")
        )
        repo.savePlayerDiscovery("bob", updated)
        val loaded = repo.loadPlayerDiscovery("bob")
        assertEquals(setOf("town:square", "town:gate"), loaded.visitedRooms)
        assertEquals(setOf("welcome", "combat_intro"), loaded.tutorials)
    }

    @Test
    fun testPlayersAreIsolated() = withTestDb {
        val repo = DiscoveryRepository()
        repo.savePlayerDiscovery("alice", PlayerDiscoveryData(
            visitedRooms = setOf("town:square", "town:gate"),
            discoveredHiddenExits = setOf("town:square:EAST"),
            discoveredLockedExits = emptySet(),
            discoveredInteractables = emptySet(),
            tutorials = setOf("welcome")
        ))
        repo.savePlayerDiscovery("bob", PlayerDiscoveryData(
            visitedRooms = setOf("forest:path"),
            discoveredHiddenExits = emptySet(),
            discoveredLockedExits = setOf("dungeon:door:NORTH"),
            discoveredInteractables = emptySet(),
            tutorials = setOf("welcome", "combat_intro")
        ))

        val aliceData = repo.loadPlayerDiscovery("alice")
        assertEquals(setOf("town:square", "town:gate"), aliceData.visitedRooms)
        assertEquals(setOf("town:square:EAST"), aliceData.discoveredHiddenExits)
        assertTrue(aliceData.discoveredLockedExits.isEmpty())
        assertEquals(setOf("welcome"), aliceData.tutorials)

        val bobData = repo.loadPlayerDiscovery("bob")
        assertEquals(setOf("forest:path"), bobData.visitedRooms)
        assertTrue(bobData.discoveredHiddenExits.isEmpty())
        assertEquals(setOf("dungeon:door:NORTH"), bobData.discoveredLockedExits)
        assertEquals(setOf("welcome", "combat_intro"), bobData.tutorials)
    }

    @Test
    fun testMarkTutorialSeen() = withTestDb {
        val repo = DiscoveryRepository()

        // Mark a tutorial as seen
        repo.markTutorialSeen("charlie", "welcome")
        val loaded = repo.loadPlayerDiscovery("charlie")
        assertEquals(setOf("welcome"), loaded.tutorials)
        // Other discovery types should be empty
        assertTrue(loaded.visitedRooms.isEmpty())
        assertTrue(loaded.discoveredHiddenExits.isEmpty())
    }

    @Test
    fun testMarkTutorialSeenIsIdempotent() = withTestDb {
        val repo = DiscoveryRepository()

        // Mark same tutorial twice — should not fail or duplicate
        repo.markTutorialSeen("charlie", "welcome")
        repo.markTutorialSeen("charlie", "welcome")
        val loaded = repo.loadPlayerDiscovery("charlie")
        assertEquals(setOf("welcome"), loaded.tutorials)
    }

    @Test
    fun testMarkTutorialSeenMultipleTutorials() = withTestDb {
        val repo = DiscoveryRepository()

        repo.markTutorialSeen("charlie", "welcome")
        repo.markTutorialSeen("charlie", "combat_intro")
        repo.markTutorialSeen("charlie", "crafting_intro")
        val loaded = repo.loadPlayerDiscovery("charlie")
        assertEquals(setOf("welcome", "combat_intro", "crafting_intro"), loaded.tutorials)
    }

    @Test
    fun testMarkTutorialSeenIsolatedPerPlayer() = withTestDb {
        val repo = DiscoveryRepository()

        repo.markTutorialSeen("alice", "welcome")
        repo.markTutorialSeen("bob", "welcome")
        repo.markTutorialSeen("bob", "combat_intro")

        assertEquals(setOf("welcome"), repo.loadPlayerDiscovery("alice").tutorials)
        assertEquals(setOf("welcome", "combat_intro"), repo.loadPlayerDiscovery("bob").tutorials)
        assertTrue(repo.loadPlayerDiscovery("charlie").tutorials.isEmpty())
    }
}
