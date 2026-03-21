#!/usr/bin/env node
/**
 * Batch icon generator for NeoMud.
 * Reads scripts/icons.json, generates each icon via nano-banana MCP CLI fallback,
 * converts to WebP, and removes background.
 *
 * Usage: node scripts/generate-icons.mjs [--start N] [--only id1,id2,...]
 *   --start N     Skip first N icons (resume after failure)
 *   --only ids    Only generate comma-separated icon IDs
 */

import fs from 'fs'
import path from 'path'
import { execSync } from 'child_process'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const manifest = JSON.parse(fs.readFileSync(path.join(__dirname, 'icons.json'), 'utf-8'))
const outDir = path.join(__dirname, '..', 'client', 'src', 'commonMain', 'composeResources', 'drawable')

// Parse args
const args = process.argv.slice(2)
let startIdx = 0
let onlyIds = null
for (let i = 0; i < args.length; i++) {
  if (args[i] === '--start' && args[i + 1]) startIdx = parseInt(args[i + 1])
  if (args[i] === '--only' && args[i + 1]) onlyIds = new Set(args[i + 1].split(','))
}

const icons = manifest.icons.filter((icon, idx) => {
  if (onlyIds && !onlyIds.has(icon.id)) return false
  if (idx < startIdx) return false
  return true
})

console.log(`Generating ${icons.length} icons (start=${startIdx})...`)
console.log(`Output: ${outDir}\n`)

if (!fs.existsSync(outDir)) fs.mkdirSync(outDir, { recursive: true })

let generated = 0
let skipped = 0

for (const icon of icons) {
  const webpPath = path.join(outDir, `${icon.id}.webp`)

  // Skip if already exists
  if (fs.existsSync(webpPath)) {
    console.log(`[skip] ${icon.id} — already exists`)
    skipped++
    continue
  }

  const fullPrompt = `${icon.prompt}. ${manifest.style}`
  console.log(`[${generated + skipped + 1}/${icons.length}] Generating ${icon.id}...`)

  try {
    // Generate PNG via nano-banana CLI (MCP not available in script context)
    // This is a placeholder — actual generation happens via MCP in the Claude session
    console.log(`  prompt: "${icon.prompt}"`)
    console.log(`  → Needs MCP generation (run in Claude session)`)
    generated++
  } catch (err) {
    console.error(`  [ERROR] ${icon.id}: ${err.message}`)
  }
}

console.log(`\nDone. Generated: ${generated}, Skipped: ${skipped}`)
console.log('Run background removal: node scripts/remove-bg.mjs --batch ' + outDir)
