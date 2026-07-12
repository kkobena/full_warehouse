#!/usr/bin/env node
'use strict';

const fs   = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const candidates = [
  path.join(root, 'src-backup', 'target', 'release', 'pharmasmart-backup.exe'),
  path.join(root, 'src-backup', 'target', 'debug', 'pharmasmart-backup.exe'),
];
const dest = path.join(root, 'src-tauri', 'backup', 'pharmasmart-backup.exe');

const src = candidates.find(fs.existsSync);

if (!src) {
  console.warn('[copy-backup-exe] pharmasmart-backup.exe absent de src-backup/target/{release,debug}/ — les tâches planifiées de sauvegarde ne pourront pas être enregistrées par l\'installeur.');
  process.exit(0);
}

const destDir = path.dirname(dest);
if (!fs.existsSync(destDir)) {
  fs.mkdirSync(destDir, { recursive: true });
}

fs.copyFileSync(src, dest);
console.log(`[copy-backup-exe] pharmasmart-backup.exe copié (${path.relative(root, src)}) → src-tauri/backup/pharmasmart-backup.exe`);
