# Playtester Agent Memory

## Characters Created
- **Brunak Ironbeard** (username: brunak01) -- DWARF WARRIOR, Level 1 (previous session)
- **Aelindra Starweave** (username: elfmage02, password: testpass123) -- ELF MAGE, Level 1, 66/100 XP
- **Gromm Thunderfist** (username: audiotest1, password: testpass123) -- HALF_ORC WARRIOR, Level 1, 329/100 XP (cannot level up)

## Areas Explored
- **Town**: Temple of the Dawn (respawn, healing aura), Town Square (trainer), Market Street (blacksmith vendor), The Enchanted Emporium (magic vendor), The Rusty Tankard (barkeep vendor), North Gate (guard)
- **Forest**: Forest Edge (rats, bandits, spiders), Winding Forest Path (hidden passage north), Sunlit Clearing (safe rest spot), Deep Forest (wolves, spiders, bandits)
- **Forest/Marsh Transition**: Overgrown Ruins (wolves, leads north to marsh)
- **Marsh**: Marsh Edge (Bog Lurkers -- 14 dmg per hit, instant death for level 1)
- **Locked**: Tavern Cellar (DOWN from tavern, "The way down is locked")

## Known Bugs Already Reported
- #53 -- Multiple relay instances corrupt shared state file
- #54 -- Forest Edge spider kills level 1 players instantly with no reaction time
- #55 -- No way to discover available spells -- mage cannot figure out what to cast
- #56 -- pickup_coins does not support "all" coin type
- #57 -- No HP regeneration outside of potions (REST skill now exists, addresses this)
- #58 -- Hostile NPCs attack immediately with no grace period for new arrivals
- #104 -- scroll_of_fireball references wrong audio path (audio/items/spell_cast.mp3 but file is in audio/spells/)
- #105 -- Missing BGM files for Marsh and Gorge zones (marsh_danger.mp3, gorge_danger.mp3)
- #107 -- Kick skill description does not mention required direction parameter
- #109 -- No way to discover level up command -- trainer says ready but won't level you up
- #110 -- Rustic Dagger uses sword sounds instead of dagger-specific sounds (dagger_slash/dagger_miss exist but unused)

## Game State Observations
- **Warrior Combat**: Iron Sword does 21-25 damage per hit (STR 35). One-shots rats (15 HP), spiders (20 HP), bandits (20 HP). Two-shots wolves (30 HP).
- **Warrior Skills**: Bash does ~23 damage. Kick does ~17 damage + requires direction (targetId:DIRECTION format). REST heals 4 HP/tick.
- **Economy**: Rat drops 1-5c, Spider drops 20-32c + Spider Fang x1-2, Bandit drops 9-12c + sometimes gear. Spider Fang sells for 2c. Health potion 20c.
- **XP**: Rats 15 XP, Bandits 18 XP, Spiders 44 XP, Wolves 28 XP. Level 2 = 100 XP total.
- **Leveling**: BLOCKED -- cannot find level up command. train_stat works but requires CP. CP requires leveling up.
- **Death**: No item/coin loss, respawn at Temple with full HP/MP
- **Marsh**: Extremely dangerous for level 1. Bog Lurker does 14 damage per hit.
- **Audio**: Sound IDs use bare names (e.g., "sword_swing") resolved to subdirectory paths by context. Server validates at startup.

## Audio Directory Structure (post-reorganization)
- `audio/bgm/` -- Background music (forest_danger, town_peaceful)
- `audio/general/` -- backstab, coin_pickup, dodge, item_pickup, loot_drop, miss, parry
- `audio/items/` -- bow_miss/shot, dagger_miss/slash, potion_drink, staff_miss/swing, sword_miss/swing
- `audio/npcs/` -- NPC attack/miss/death/interact/exit sounds
- `audio/rooms/` -- footstep_* depart sounds
- `audio/spells/` -- spell cast/impact/fizzle sounds, healing_aura

## TODOs
- Test with a caster class to verify spell sounds work in combat
- Test multiplayer interactions
- Explore Blackstone Gorge
- Try leveling up once the bug is fixed
