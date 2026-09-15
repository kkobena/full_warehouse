#!/usr/bin/env node
'use strict';

/**
 * Suffixe l'installeur NSIS qui vient d'être produit, pour distinguer des
 * déclinaisons qui portent sinon exactement le même nom de fichier.
 *
 * Le produit serveur existe en deux déclinaisons — avec JRE embarqué et sans —
 * issues de deux fichiers de configuration Tauri différents mais du même
 * `productName` et de la même version. Elles produisent donc toutes les deux
 * `PharmaSmart_<version>_x64-setup.exe`. Comme les versions sont acheminées chez
 * les clients par transfert réseau, deux fichiers homonymes finissent tôt ou tard
 * appliqués l'un pour l'autre : appliquer la déclinaison sans JRE sur une
 * installation avec JRE supprime le JRE d'un poste qui n'a pas de Java système
 * (cf. la garde JRE_FLAG dans installer-hooks/installer.nsh).
 *
 * Usage : node scripts/label-installer.js <suffixe>
 *   node scripts/label-installer.js avec-jre
 *     → PharmaSmart_1.2.3_x64-setup.exe  devient
 *       PharmaSmart_1.2.3_x64-setup-avec-jre.exe
 */

const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const nsisDir = path.join(root, 'src-tauri', 'target', 'release', 'bundle', 'nsis');

const suffix = process.argv[2];
if (!suffix) {
  console.error('[label-installer] suffixe manquant. Usage : node scripts/label-installer.js <suffixe>');
  process.exit(1);
}

if (!fs.existsSync(nsisDir)) {
  console.warn(`[label-installer] ${nsisDir} absent — rien à renommer.`);
  process.exit(0);
}

// On ne renomme que les installeurs non encore suffixés, pour rester idempotent
// si le script est relancé sans nouveau build.
const candidates = fs
  .readdirSync(nsisDir)
  .filter(name => name.endsWith('-setup.exe'))
  .map(name => ({ name, mtime: fs.statSync(path.join(nsisDir, name)).mtimeMs }))
  .sort((a, b) => b.mtime - a.mtime);

if (candidates.length === 0) {
  console.warn('[label-installer] aucun installeur *-setup.exe trouvé — rien à renommer.');
  process.exit(0);
}

const source = candidates[0].name;
const target = source.replace(/-setup\.exe$/, `-setup-${suffix}.exe`);

fs.renameSync(path.join(nsisDir, source), path.join(nsisDir, target));
console.log(`[label-installer] ${source} → ${target}`);
