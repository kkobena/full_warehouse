#!/usr/bin/env node
'use strict';

/**
 * Publie l'exécutable CLIENT dans le relais de mise à jour.
 *
 * Les postes clients ne sont pas installés : on y dépose `pharmasmart.exe`. La mise
 * à jour consiste donc à remplacer ce fichier — c'est le mécanisme de
 * `src-tauri/src/self_update.rs`, pas celui de `tauri-plugin-updater`, qui ne sait
 * mettre à jour qu'une application installée.
 *
 * Ce script copie `target/release/pharmasmart.exe` dans `src-tauri/updates/` sous le
 * nom `pharmasmart-<version>.exe`. Le numéro dans le nom n'est pas décoratif : c'est
 * ce qui permet au backend d'annoncer la version disponible, le binaire brut ne
 * portant pas cette information de façon exploitable.
 *
 * Le répertoire `updates/` est ensuite embarqué dans les installeurs SERVEUR, qui le
 * déposent dans %PROGRAMDATA%/PharmaSmart/updates où le backend le publie.
 *
 * ⚠ ÉTAPE FACULTATIVE — c'est le chemin de retour à l'existant. Non jouée, `updates/`
 * ne contient que son README, le backend ne propose rien, et les postes clients
 * restent mis à jour par dépôt manuel du fichier.
 */

const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const releaseExe = path.join(root, 'src-tauri', 'target', 'release', 'pharmasmart.exe');
const updatesDir = path.join(root, 'src-tauri', 'updates');

const version = require(path.join(root, 'package.json')).version;

fs.mkdirSync(updatesDir, { recursive: true });

if (!fs.existsSync(releaseExe)) {
  console.warn(`[client-update] ${path.relative(root, releaseExe)} absent — rien à publier.`);
  console.warn("[client-update] Lancer d'abord : npm run tauri:build");
  process.exit(0);
}

// Le catalogue ne publie qu'une version : sans ce ménage, les anciens binaires
// s'accumuleraient et grossiraient l'installeur serveur à chaque release.
for (const stale of fs.readdirSync(updatesDir)) {
  if (stale.endsWith('.exe')) {
    fs.unlinkSync(path.join(updatesDir, stale));
    console.log(`[client-update] ancien artefact retiré : ${stale}`);
  }
}

const publishedName = `pharmasmart-${version}.exe`;
fs.copyFileSync(releaseExe, path.join(updatesDir, publishedName));

console.log(`[client-update] publié : ${publishedName}`);
console.log('[client-update] Prêt. Les installeurs serveur embarqueront cet artefact.');
