/* eslint-disable no-console */
/**
 * Synchronise la version de l'application depuis la source unique de vérité
 * (pom.xml → <revision>) vers :
 *   - package.json                       (fallback __VERSION__ + métadonnées npm)
 *   - src-tauri/tauri.conf.json          (build Tauri standard)
 *   - src-tauri/tauri.bundled.conf.json  (installeur backend embarqué)
 *   - src-tauri/tauri.bundled-jre.conf.json (installeur backend + JRE)
 */
const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');

/** Lit la version canonique depuis pom.xml → <revision>X.Y.Z</revision>. */
function readCanonicalVersion() {
  const pomPath = path.join(ROOT, 'pom.xml');
  const pom = fs.readFileSync(pomPath, 'utf8');
  const match = pom.match(/<revision>\s*([^<\s]+)\s*<\/revision>/);
  if (!match) {
    throw new Error(`<revision> introuvable dans ${pomPath}`);
  }
  const version = match[1].trim();

  if (!/^\d+\.\d+\.\d+(?:[-+].+)?$/.test(version)) {
    throw new Error(`Version "${version}" non conforme au semver attendu (X.Y.Z) par Tauri.`);
  }
  return version;
}

/**
 * bundling Tauri échoue. Maven et l'affichage in-app conservent la version
 * complète via package.json.
 */
function toTauriVersion(version) {
  return version.replace(/[-+].*$/, '');
}

/** Remplace la PREMIÈRE occurrence du champ "version": "..." dans un fichier. */
function updateVersionField(relPath, version) {
  const filePath = path.join(ROOT, relPath);
  if (!fs.existsSync(filePath)) {
    console.warn(`  ⚠  ${relPath} introuvable — ignoré`);
    return;
  }
  const original = fs.readFileSync(filePath, 'utf8');
  let replaced = false;
  const updated = original.replace(/("version"\s*:\s*")([^"]*)(")/, (full, p1, old, p3) => {
    replaced = true;
    if (old === version) {
      return full; // déjà à jour
    }
    return `${p1}${version}${p3}`;
  });

  if (!replaced) {
    console.warn(`  ⚠  champ "version" non trouvé dans ${relPath} — ignoré`);
    return;
  }
  if (updated !== original) {
    fs.writeFileSync(filePath, updated);
    console.log(`  ✓ ${relPath} → ${version}`);
  } else {
    console.log(`  = ${relPath} déjà à ${version}`);
  }
}

function main() {
  const version = readCanonicalVersion();
  const tauriVersion = toTauriVersion(version);
  console.log(`Synchronisation de la version applicative : ${version} (source : pom.xml <revision>)`);
  if (tauriVersion !== version) {
    console.log(`  → version Tauri/installeur normalisée (numérique) : ${tauriVersion}`);
  }

  // package.json conserve la version complète (affichage in-app via __VERSION__).
  updateVersionField('package.json', version);
  // Les confs Tauri reçoivent la version numérique (contrainte MSI/NSIS).
  updateVersionField('src-tauri/tauri.conf.json', tauriVersion);
  updateVersionField('src-tauri/tauri.bundled.conf.json', tauriVersion);
  updateVersionField('src-tauri/tauri.bundled-jre.conf.json', tauriVersion);

  console.log('Version synchronisée.');
}

main();

