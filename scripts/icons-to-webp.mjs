#!/usr/bin/env node
/**
 * Convert bg-removed PNGs to 64x64 WebP with icon ID names.
 * Run AFTER bg removal on full-size PNGs.
 */
import fs from 'fs'
import path from 'path'
import { execSync } from 'child_process'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const srcDir = path.join(__dirname, '..', 'nanobanana-output')
const dstDir = path.join(__dirname, '..', 'client', 'src', 'commonMain', 'composeResources', 'drawable')

const fileToId = {
  'a_single_leather_backpack_with_b': 'icon_inventory',
  'a_single_medieval_kite_shield_wi': 'icon_equipment',
  'a_single_rolled_parchment_treasu': 'icon_map',
  'a_single_ornate_bronze_gear_cog_': 'icon_settings',
  'a_single_clenched_iron_gauntlet_': 'icon_bash',
  'a_single_armored_boot_midkick_wi': 'icon_kick',
  'a_single_dark_hooded_cloak_with_': 'icon_sneak',
  'a_single_figure_sitting_crossleg': 'icon_meditate',
  'a_single_glowing_animal_pawprint': 'icon_track',
  'a_single_lockpick_tool_inserted_': 'icon_picklock',
  'a_single_campfire_with_warm_oran': 'icon_rest',
  'a_single_pair_of_crossed_swords_': 'icon_attack',
  'a_single_bubbling_green_poison_v': 'icon_effect_poison',
  'a_single_glowing_golden_heart_wi': 'icon_effect_heal_over_time',
  'a_single_flexing_muscular_arm_wi': 'icon_effect_buff_strength',
  'a_single_pair_of_winged_boots_wi': 'icon_effect_buff_agility',
  'a_single_glowing_open_spellbook_': 'icon_effect_buff_intellect',
  'a_single_glowing_third_eye_symbo': 'icon_effect_buff_willpower',
  'a_single_golden_hourglass_with_s': 'icon_effect_haste',
  'a_single_burning_flame_with_jagg': 'icon_effect_damage',
  'a_single_blue_water_droplet_with': 'icon_effect_mana_regen',
  'a_single_cracked_purple_crystal_': 'icon_effect_mana_drain',
  'a_single_stack_of_gold_coins_wit': 'icon_vendor',
  'a_single_wooden_training_dummy_w': 'icon_trainer',
  'a_single_blacksmith_anvil_with_h': 'icon_crafter',
  'a_single_open_wooden_door_with_l': 'icon_exit_open',
  'a_single_open_treasure_chest_ove': 'icon_treasure_drop',
  'a_single_red_warning_triangle_wi': 'icon_monster_spawn',
  'a_single_swirling_magical_aura_o': 'icon_room_effect',
  'a_single_glowing_blue_portal_vor': 'icon_teleport',
  'a_single_glowing_hand_reaching_t': 'icon_interact',
  'a_single_pointed_wizard_hat_with': 'icon_school_mage',
  'a_single_radiant_golden_sun_with': 'icon_school_priest',
  'a_single_oak_leaf_with_vines_and': 'icon_school_druid',
  'a_single_chi_flame_fist_with_inn': 'icon_school_kai',
  'a_single_golden_lyre_harp_with_m': 'icon_school_bard',
  'a_single_fivepointed_star_with_m': 'icon_school_default',
  'a_single_glowing_arcane_energy_b': 'icon_spell_magic_missile',
  'a_single_translucent_blue_magica': 'icon_spell_arcane_shield',
  'a_single_sharp_ice_crystal_shard': 'icon_spell_frost_bolt',
  'a_single_blazing_fireball_with_o': 'icon_spell_fireball',
  'a_single_golden_lightning_bolt_s': 'icon_spell_smite',
  'a_single_brilliant_whitegold_sun': 'icon_spell_holy_smite',
  'a_single_small_green_healing_cro': 'icon_spell_minor_heal',
  'a_single_open_palm_with_golden_b': 'icon_spell_blessing',
  'a_single_pair_of_glowing_hands_c': 'icon_spell_cure_wounds',
  'a_single_brilliant_radiant_sun_w': 'icon_spell_divine_light',
  'a_single_thorny_green_vine_whip_': 'icon_spell_thorn_strike',
  'a_single_green_leaf_with_dewdrop': 'icon_spell_healing_touch',
  'a_single_billowing_greenpurple_t': 'icon_spell_poison_cloud',
  'a_single_massive_gnarled_tree_ro': 'icon_spell_natures_wrath',
  'a_single_meditating_figure_silho': 'icon_spell_inner_fire',
  'a_single_open_palm_thrust_with_b': 'icon_spell_chi_strike',
  'a_single_concentrated_energy_sph': 'icon_spell_ki_blast',
  'a_single_crystalline_diamond_hum': 'icon_spell_diamond_body',
  'a_single_sharp_musical_note_with': 'icon_spell_cutting_words',
  'a_single_golden_trophy_chalice_w': 'icon_spell_inspire',
  'a_single_gentle_bluegreen_musica': 'icon_spell_soothing_song',
  'a_single_shattered_musical_note_': 'icon_spell_discord',
  'a_single_war_horn_with_golden_so': 'icon_spell_rallying_cry',
  'a_single_sword_wreathed_in_red_f': 'icon_buff_damage',
  'a_single_glowing_red_heart_with_': 'icon_buff_max_hp',
}

if (!fs.existsSync(dstDir)) fs.mkdirSync(dstDir, { recursive: true })

let ok = 0
for (const [stem, iconId] of Object.entries(fileToId)) {
  const srcPath = path.join(srcDir, `${stem}.png`)
  const dstPath = path.join(dstDir, `${iconId}.webp`)
  if (!fs.existsSync(srcPath)) { console.log(`[MISS] ${stem}.png`); continue }
  try {
    execSync(`npx sharp-cli -i "${srcPath}" -o "${dstPath}" --format webp -- resize 64 64 --fit cover`, { stdio: 'pipe' })
    console.log(`[ok] ${iconId}.webp`)
    ok++
  } catch (err) {
    console.error(`[ERR] ${iconId}: ${err.message?.substring(0, 80)}`)
  }
}
console.log(`\nDone: ${ok}/${Object.keys(fileToId).length} icons`)
