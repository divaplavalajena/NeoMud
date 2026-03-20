package com.neomud.server.game.progression

import com.neomud.server.game.GameConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeathXpPenaltyTest {

    @Test
    fun xp_penalty_is_5_percent_of_current_xp() {
        val currentXp = 1000L
        val penalty = (currentXp * GameConfig.Progression.DEATH_XP_LOSS_PERCENT).toLong()
        assertEquals(50L, penalty)
    }

    @Test
    fun xp_after_penalty_cannot_go_below_zero() {
        val currentXp = 5L
        val penalty = (currentXp * GameConfig.Progression.DEATH_XP_LOSS_PERCENT).toLong()
        val newXp = (currentXp - penalty).coerceAtLeast(0L)
        assertTrue(newXp >= 0)
    }

    @Test
    fun death_can_drop_xp_below_level_threshold() {
        // Player with 531 XP, threshold is 530 — was the reported bug scenario
        val currentXp = 531L
        val xpToNextLevel = 530L
        assertTrue(XpCalculator.isReadyToLevel(currentXp, xpToNextLevel, level = 2))

        // After death: 5% penalty = 26 XP lost → 505 XP
        val penalty = (currentXp * GameConfig.Progression.DEATH_XP_LOSS_PERCENT).toLong()
        val newXp = (currentXp - penalty).coerceAtLeast(0L)
        assertEquals(505L, newXp)

        // No longer ready to level — this was the bug: client showed 531 but server had 505
        assertTrue(!XpCalculator.isReadyToLevel(newXp, xpToNextLevel, level = 2))
    }

    @Test
    fun level_1_players_are_exempt_from_death_penalty() {
        val level = 1
        val currentXp = 500L
        // Level 1 is below DEATH_XP_PENALTY_MIN_LEVEL
        val xpPenalty = if (level >= GameConfig.Progression.DEATH_XP_PENALTY_MIN_LEVEL) {
            (currentXp * GameConfig.Progression.DEATH_XP_LOSS_PERCENT).toLong()
        } else 0L
        assertEquals(0L, xpPenalty)
    }

    @Test
    fun level_2_players_do_get_death_penalty() {
        val level = 2
        val currentXp = 500L
        val xpPenalty = if (level >= GameConfig.Progression.DEATH_XP_PENALTY_MIN_LEVEL) {
            (currentXp * GameConfig.Progression.DEATH_XP_LOSS_PERCENT).toLong()
        } else 0L
        assertEquals(25L, xpPenalty)
    }

    @Test
    fun zero_xp_produces_zero_penalty() {
        val currentXp = 0L
        val penalty = (currentXp * GameConfig.Progression.DEATH_XP_LOSS_PERCENT).toLong()
        assertEquals(0L, penalty)
    }
}
