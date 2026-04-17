#!/usr/bin/env node

/**
 * Prepare Backup Script
 * ---------------------
 * Compiles the Rust backup tool (`src-backup`) in release mode and copies:
 *   - pharmasmart-backup.exe
 *   - setup-backup-tasks.ps1
 *   - remove-backup-tasks.ps1
 * to `src-tauri/backup/` so they can be bundled by Tauri as resources.
 *
 * See docs/BACKUP-STRATEGY.md.
 */

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const projectRoot = path.resolve(__dirname, '..');
const backupSrc = path.join(projectRoot, 'src-backup');
const backupDst = path.join(projectRoot, 'src-tauri', 'backup');

const exeName = process.platform === 'win32' ? 'pharmasmart-backup.exe' : 'pharmasmart-backup';
const builtExe = path.join(backupSrc, 'target', 'release', exeName);

console.log('Preparing pharmasmart-backup for Tauri bundle...');

if (!fs.existsSync(backupSrc)) {
  console.error(`src-backup not found: ${backupSrc}`);
  process.exit(1);
}

fs.mkdirSync(backupDst, { recursive: true });

// 1. Build Rust project (release)
console.log('Building src-backup (cargo build --release)...');
try {
  execSync('cargo build --release', { cwd: backupSrc, stdio: 'inherit' });
} catch (err) {
  console.error(`cargo build failed: ${err.message}`);
  process.exit(1);
}

if (!fs.existsSync(builtExe)) {
  console.error(`Built binary not found: ${builtExe}`);
  process.exit(1);
}

// 2. Copy exe
const dstExe = path.join(backupDst, exeName);
fs.copyFileSync(builtExe, dstExe);
console.log(`  ${exeName} -> ${dstExe}`);

// 3. Copy PowerShell scripts
const psScripts = ['setup-backup-tasks.ps1', 'remove-backup-tasks.ps1'];
for (const script of psScripts) {
  const src = path.join(backupSrc, 'scripts', script);
  const dst = path.join(backupDst, script);
  fs.copyFileSync(src, dst);
  console.log(`  ${script} -> ${dst}`);
}

console.log('Backup resources ready in src-tauri/backup/.');
