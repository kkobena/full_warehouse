#!/usr/bin/env node
// Wrapper autour de `ng build` / `ng serve` — injecte I18N_HASH et __VERSION__ dynamiquement
// via --define (répétable, cf. `ng build --help`), pour reproduire le comportement de l'ancien
// webpack/environment.js qui lisait ces valeurs à chaque build plutôt qu'un define statique figé
// dans angular.json. Invoque le binaire Angular CLI en argv (execFileSync), sans shell, pour
// éviter les soucis de quoting entre bash/PowerShell/cmd sur Windows (cf. PLAN §7).
const { execFileSync } = require('child_process');
const path = require('path');
const fs = require('fs');

const HASH_FILE = path.resolve(__dirname, '..', 'target', '.i18n-hash.json');
const PACKAGE_JSON = path.resolve(__dirname, '..', 'package.json');
const NG_BIN = require.resolve('@angular/cli/bin/ng.js');

function readI18nHash() {
  if (!fs.existsSync(HASH_FILE)) {
    throw new Error(`Fichier hash i18n introuvable: ${HASH_FILE} — lancer "npm run build:i18n" d'abord.`);
  }
  return JSON.parse(fs.readFileSync(HASH_FILE, 'utf8')).hash;
}

function readVersion() {
  return JSON.parse(fs.readFileSync(PACKAGE_JSON, 'utf8')).version;
}

function main() {
  const [command, configuration, ...rest] = process.argv.slice(2);
  if (!command || !['build', 'serve'].includes(command)) {
    console.error('Usage: node scripts/build-app.js <build|serve> <configuration> [...ng args]');
    process.exit(1);
  }

  const hash = readI18nHash();
  const version = process.env.APP_VERSION || readVersion();

  const args = [
    command,
    '--configuration',
    configuration,
    '--define',
    `I18N_HASH="${hash}"`,
    '--define',
    `__VERSION__="${version}"`,
    ...rest,
  ];

  execFileSync(process.execPath, [NG_BIN, ...args], { stdio: 'inherit' });
}

main();
