#!/usr/bin/env node
/**
 * Removes backgrounds from AI-generated sprites using either edge-based
 * white flood-fill or ML-based segmentation.
 *
 * --white mode (recommended for sprites with solid white backgrounds):
 *   Flood-fills from image edges through white/near-white pixels only.
 *   Preserves interior white (gaps between arms, weapons, bow strings).
 *   Applies graduated alpha at boundaries for smooth anti-aliased edges.
 *   No external dependencies beyond sharp.
 *
 * Default mode (ML-based, for non-white backgrounds):
 *   Uses rembg (birefnet-general model) via uvx for foreground segmentation.
 *   Prerequisite: uv/uvx must be installed (https://docs.astral.sh/uv/)
 *
 * Usage: node scripts/remove-bg.mjs --white <input.webp>
 *        node scripts/remove-bg.mjs --white --batch <directory>
 *        node scripts/remove-bg.mjs <input.webp>
 *        node scripts/remove-bg.mjs --batch <directory> [--ext webp]
 *        node scripts/remove-bg.mjs --preview <input.webp>
 *        node scripts/remove-bg.mjs --suffix nobg <input.webp>
 *
 * Options:
 *   --white             Use edge flood-fill white removal (fast, preserves interiors)
 *   --threshold <n>     White detection threshold 0-255 (default: 240)
 *   --batch <dir>       Process all matching files in directory
 *   --ext <ext>         File extension filter for batch mode (default: webp)
 *   --preview           Generate side-by-side before/after comparison
 *   --suffix <str>      Output to {name}_{suffix}.webp instead of overwriting
 */

import sharp from 'sharp';
import { execFile } from 'child_process';
import { readdir, readFile, writeFile, unlink } from 'fs/promises';
import { join, extname, basename, dirname } from 'path';
import { tmpdir } from 'os';
import { randomBytes } from 'crypto';
import { promisify } from 'util';

const execFileAsync = promisify(execFile);

const UVX = process.platform === 'win32' ? 'uvx.exe' : 'uvx';
const DEFAULT_WHITE_THRESHOLD = 240;
const INTERIOR_REGION_MIN_PIXELS = 50; // white regions larger than this get removed in pass 2

// --- White edge flood-fill mode ---

async function removeWhiteBackground(inputPath, outputPath, options = {}) {
  const actualOutput = resolveOutput(inputPath, outputPath, options);
  const threshold = options.threshold || DEFAULT_WHITE_THRESHOLD;

  // Read into buffer first to avoid file locking on Windows
  const inputBuffer = await readFile(inputPath);
  const meta = await sharp(inputBuffer).metadata();
  const w = meta.width;
  const h = meta.height;

  // Get raw RGBA pixel data
  const { data, info } = await sharp(inputBuffer)
    .ensureAlpha()
    .raw()
    .toBuffer({ resolveWithObject: true });

  const pixels = new Uint8Array(data);
  const totalPixels = w * h;

  // Track which pixels to make transparent (flood-filled from edges)
  const visited = new Uint8Array(totalPixels);
  const toRemove = new Uint8Array(totalPixels);

  function isWhite(idx) {
    const off = idx * 4;
    return pixels[off] >= threshold &&
           pixels[off + 1] >= threshold &&
           pixels[off + 2] >= threshold;
  }

  // BFS flood-fill from all edge pixels that are white
  const queue = [];

  // Seed with all white edge pixels
  for (let x = 0; x < w; x++) {
    // Top row
    const topIdx = x;
    if (isWhite(topIdx)) { queue.push(topIdx); visited[topIdx] = 1; }
    // Bottom row
    const botIdx = (h - 1) * w + x;
    if (isWhite(botIdx)) { queue.push(botIdx); visited[botIdx] = 1; }
  }
  for (let y = 1; y < h - 1; y++) {
    // Left column
    const leftIdx = y * w;
    if (isWhite(leftIdx)) { queue.push(leftIdx); visited[leftIdx] = 1; }
    // Right column
    const rightIdx = y * w + (w - 1);
    if (isWhite(rightIdx)) { queue.push(rightIdx); visited[rightIdx] = 1; }
  }

  // BFS through connected white pixels
  let head = 0;
  while (head < queue.length) {
    const idx = queue[head++];
    toRemove[idx] = 1;

    const x = idx % w;
    const y = (idx - x) / w;

    // 4-connected neighbors
    const neighbors = [];
    if (x > 0) neighbors.push(idx - 1);
    if (x < w - 1) neighbors.push(idx + 1);
    if (y > 0) neighbors.push(idx - w);
    if (y < h - 1) neighbors.push(idx + w);

    for (const nIdx of neighbors) {
      if (!visited[nIdx] && isWhite(nIdx)) {
        visited[nIdx] = 1;
        queue.push(nIdx);
      }
    }
  }

  // Pass 2: find remaining white regions not reached by edge flood-fill
  // Remove any connected white region larger than INTERIOR_REGION_MIN_PIXELS
  const regionId = new Int32Array(totalPixels); // 0 = unassigned
  let nextRegion = 1;

  for (let i = 0; i < totalPixels; i++) {
    if (toRemove[i] || regionId[i] || !isWhite(i)) continue;

    // BFS to find this connected white region
    const regionPixels = [];
    const rQueue = [i];
    regionId[i] = nextRegion;
    let rHead = 0;

    while (rHead < rQueue.length) {
      const idx = rQueue[rHead++];
      regionPixels.push(idx);

      const x = idx % w;
      const y = (idx - x) / w;

      const neighbors = [];
      if (x > 0) neighbors.push(idx - 1);
      if (x < w - 1) neighbors.push(idx + 1);
      if (y > 0) neighbors.push(idx - w);
      if (y < h - 1) neighbors.push(idx + w);

      for (const nIdx of neighbors) {
        if (!regionId[nIdx] && !toRemove[nIdx] && isWhite(nIdx)) {
          regionId[nIdx] = nextRegion;
          rQueue.push(nIdx);
        }
      }
    }

    // Remove large white regions (likely background pockets)
    if (regionPixels.length >= INTERIOR_REGION_MIN_PIXELS) {
      for (const idx of regionPixels) {
        toRemove[idx] = 1;
      }
    }

    nextRegion++;
  }

  // Apply transparency with graduated alpha at boundaries
  // For each pixel marked for removal: fully transparent
  // For near-white pixels adjacent to removed pixels: graduated alpha
  for (let i = 0; i < totalPixels; i++) {
    if (toRemove[i]) {
      pixels[i * 4 + 3] = 0; // fully transparent
    }
  }

  // Anti-aliasing pass: for non-removed pixels that are near-white and
  // adjacent to a removed pixel, apply graduated alpha based on brightness
  const softenThreshold = 220; // pixels above this near the edge get partial alpha
  for (let i = 0; i < totalPixels; i++) {
    if (toRemove[i]) continue;

    const x = i % w;
    const y = (i - x) / w;
    const off = i * 4;
    const r = pixels[off], g = pixels[off + 1], b = pixels[off + 2];
    const brightness = (r + g + b) / 3;

    if (brightness < softenThreshold) continue;

    // Check if any neighbor was removed
    let adjacentToRemoved = false;
    if (x > 0 && toRemove[i - 1]) adjacentToRemoved = true;
    else if (x < w - 1 && toRemove[i + 1]) adjacentToRemoved = true;
    else if (y > 0 && toRemove[i - w]) adjacentToRemoved = true;
    else if (y < h - 1 && toRemove[i + w]) adjacentToRemoved = true;

    if (adjacentToRemoved) {
      // Graduated alpha: brightness 220→240 maps to alpha 255→0
      const range = threshold - softenThreshold;
      const fade = Math.max(0, Math.min(1, (brightness - softenThreshold) / range));
      pixels[off + 3] = Math.round(255 * (1 - fade));
    }
  }

  const resultBuffer = Buffer.from(pixels);

  // Generate preview if requested
  if (options.preview) {
    const pngBuffer = await sharp(resultBuffer, { raw: { width: w, height: h, channels: 4 } })
      .png()
      .toBuffer();
    await generatePreview(inputBuffer, pngBuffer, inputPath);
  }

  // Write output
  const outExt = extname(actualOutput).toLowerCase();
  let outBuffer;
  if (outExt === '.png') {
    outBuffer = await sharp(resultBuffer, { raw: { width: w, height: h, channels: 4 } })
      .png()
      .toBuffer();
  } else {
    outBuffer = await sharp(resultBuffer, { raw: { width: w, height: h, channels: 4 } })
      .webp({ lossless: true })
      .toBuffer();
  }

  await writeFile(actualOutput, outBuffer);
  console.log(`  ✓ ${basename(actualOutput)}`);
}

// --- ML-based mode (rembg) ---

async function runRembg(inputPath, outputPngPath) {
  await execFileAsync(UVX, [
    '--with', 'rembg[cpu,cli]',
    'rembg', 'i',
    '-a',
    '-m', 'birefnet-general',
    inputPath, outputPngPath,
  ], { timeout: 120_000 });
}

async function removeBackgroundML(inputPath, outputPath, options = {}) {
  const actualOutput = resolveOutput(inputPath, outputPath, options);

  const tempId = randomBytes(6).toString('hex');
  const tempPng = join(tmpdir(), `rembg_${tempId}.png`);

  try {
    let rembgInput = inputPath;
    let tempInput = null;
    const inputExt = extname(inputPath).toLowerCase();
    if (inputExt === '.webp') {
      tempInput = join(tmpdir(), `rembg_in_${tempId}.png`);
      await sharp(inputPath).png().toFile(tempInput);
      rembgInput = tempInput;
    }

    await runRembg(rembgInput, tempPng);

    if (tempInput) {
      await unlink(tempInput).catch(() => {});
    }

    const resultBuffer = await readFile(tempPng);

    if (options.preview) {
      const inputBuffer = await readFile(inputPath);
      await generatePreview(inputBuffer, resultBuffer, inputPath);
    }

    const outExt = extname(actualOutput).toLowerCase();
    let outBuffer;
    if (outExt === '.png') {
      outBuffer = resultBuffer;
    } else {
      outBuffer = await sharp(resultBuffer).webp({ lossless: true }).toBuffer();
    }

    await writeFile(actualOutput, outBuffer);
    console.log(`  ✓ ${basename(actualOutput)}`);
  } finally {
    await unlink(tempPng).catch(() => {});
  }
}

// --- Shared utilities ---

function resolveOutput(inputPath, outputPath, options) {
  if (options.suffix) {
    const dir = dirname(inputPath);
    const base = basename(inputPath, extname(inputPath));
    return join(dir, `${base}_${options.suffix}.webp`);
  }
  return outputPath || inputPath;
}

async function generatePreview(originalBuffer, processedBuffer, inputPath) {
  const original = sharp(originalBuffer);
  const processed = sharp(processedBuffer);

  const origMeta = await original.metadata();
  const w = origMeta.width;
  const h = origMeta.height;

  const checkerSize = 16;
  const checkerBuffer = Buffer.alloc(w * h * 4);
  for (let y = 0; y < h; y++) {
    for (let x = 0; x < w; x++) {
      const idx = (y * w + x) * 4;
      const isLight = ((Math.floor(x / checkerSize) + Math.floor(y / checkerSize)) % 2) === 0;
      const val = isLight ? 220 : 180;
      checkerBuffer[idx] = val;
      checkerBuffer[idx + 1] = val;
      checkerBuffer[idx + 2] = val;
      checkerBuffer[idx + 3] = 255;
    }
  }

  const checkerBg = await sharp(checkerBuffer, { raw: { width: w, height: h, channels: 4 } })
    .composite([{ input: await processed.resize(w, h).toBuffer(), blend: 'over' }])
    .png()
    .toBuffer();

  const previewBuffer = await sharp({
    create: { width: w * 2 + 4, height: h, channels: 4, background: { r: 40, g: 40, b: 40, alpha: 255 } }
  })
    .composite([
      { input: await original.png().toBuffer(), left: 0, top: 0 },
      { input: checkerBg, left: w + 4, top: 0 },
    ])
    .png()
    .toBuffer();

  const previewName = basename(inputPath, extname(inputPath)) + '_preview.png';
  const previewPath = join(dirname(inputPath), previewName);
  await writeFile(previewPath, previewBuffer);
  console.log(`  📋 Preview: ${previewPath}`);
}

async function processDirectory(dir, ext = 'webp', options = {}) {
  const files = await readdir(dir);
  const targets = files.filter(f =>
    f.endsWith(`.${ext}`) &&
    !f.startsWith('.') &&
    !f.includes('_preview') &&
    !(options.suffix && f.includes(`_${options.suffix}.`))
  );

  console.log(`Processing ${targets.length} .${ext} files in ${dir}...`);

  const removeFn = options.white ? removeWhiteBackground : removeBackgroundML;

  for (const file of targets) {
    const fullPath = join(dir, file);
    try {
      await removeFn(fullPath, fullPath, options);
    } catch (err) {
      console.error(`  ✗ ${file}: ${err.message}`);
    }
  }
}

// --- CLI ---

const args = process.argv.slice(2);
const options = {};
const positional = [];

for (let i = 0; i < args.length; i++) {
  if (args[i] === '--white') {
    options.white = true;
  } else if (args[i] === '--threshold' && args[i + 1]) {
    options.threshold = parseInt(args[++i], 10);
  } else if (args[i] === '--suffix' && args[i + 1]) {
    options.suffix = args[++i];
  } else if (args[i] === '--preview') {
    options.preview = true;
  } else if (args[i] === '--batch') {
    options.batch = true;
    if (args[i + 1] && !args[i + 1].startsWith('--')) {
      positional.push(args[++i]);
    }
  } else if (args[i] === '--ext' && args[i + 1]) {
    options.ext = args[++i];
  } else if (!args[i].startsWith('--')) {
    positional.push(args[i]);
  }
}

const removeFn = options.white ? removeWhiteBackground : removeBackgroundML;

if (options.batch && positional.length >= 1) {
  await processDirectory(positional[0], options.ext || 'webp', options);
} else if (!options.batch && positional.length >= 1) {
  await removeFn(positional[0], positional[1] || positional[0], options);
} else {
  console.log('Usage:');
  console.log('  node scripts/remove-bg.mjs --white <input> [output]');
  console.log('  node scripts/remove-bg.mjs --white --batch <directory>');
  console.log('  node scripts/remove-bg.mjs <input> [output]        (ML mode, requires uvx)');
  console.log('  node scripts/remove-bg.mjs --batch <directory>     (ML mode)');
  console.log('  node scripts/remove-bg.mjs --preview <input>');
  console.log('  node scripts/remove-bg.mjs --suffix nobg <input>');
  console.log('  node scripts/remove-bg.mjs --threshold 230 --white <input>');
}
