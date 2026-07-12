#!/usr/bin/env node
'use strict';

const fs   = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const src  = path.join(root, 'service', 'WinSW.exe');
const dest = path.join(root, 'src-tauri', 'service', 'WinSW.exe');

if (!fs.existsSync(src)) {
  console.warn('[copy-winsw] WinSW.exe absent de service/ — l\'installation du service Windows utilisera le téléchargement en ligne.');
  process.exit(0);
}

const destDir = path.dirname(dest);
if (!fs.existsSync(destDir)) {
  fs.mkdirSync(destDir, { recursive: true });
}

fs.copyFileSync(src, dest);
console.log('[copy-winsw] WinSW.exe copié → src-tauri/service/WinSW.exe');
