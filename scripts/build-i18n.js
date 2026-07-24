#!/usr/bin/env node
// Fusion des JSON i18n (fr/, en/) en fr.json / en.json — remplace MergeJsonWebpackPlugin.
// Fusion PROFONDE (deep merge) : chaque fichier expose une clé racine (warehouseApp, error, ...)
// qui doit être mergée avec celles des autres fichiers, pas imbriquée sous le nom du fichier.
const fs = require('fs');
const path = require('path');
const { hashElement } = require('folder-hash');

const ROOT = path.resolve(__dirname, '..', 'pharmaSmart-app', 'src', 'main', 'webapp', 'i18n');
const LANGS = ['fr', 'en'];
const HASH_OUTPUT = path.resolve(__dirname, '..', 'target', '.i18n-hash.json');

function deepMerge(target, source) {
  for (const key of Object.keys(source)) {
    if (
      source[key] &&
      typeof source[key] === 'object' &&
      !Array.isArray(source[key]) &&
      target[key] &&
      typeof target[key] === 'object' &&
      !Array.isArray(target[key])
    ) {
      deepMerge(target[key], source[key]);
    } else {
      target[key] = source[key];
    }
  }
  return target;
}

async function main() {
  const hash = (
    await hashElement(ROOT, {
      algo: 'md5',
      encoding: 'hex',
      files: { include: ['*.json'] },
    })
  ).hash;

  for (const lang of LANGS) {
    const dir = path.join(ROOT, lang);
    if (!fs.existsSync(dir)) continue;
    const files = fs.readdirSync(dir).filter(f => f.endsWith('.json'));
    const merged = {};
    for (const f of files) {
      const content = JSON.parse(fs.readFileSync(path.join(dir, f), 'utf8'));
      deepMerge(merged, content);
    }
    fs.writeFileSync(path.join(ROOT, `${lang}.json`), JSON.stringify(merged));
  }

  fs.mkdirSync(path.dirname(HASH_OUTPUT), { recursive: true });
  fs.writeFileSync(HASH_OUTPUT, JSON.stringify({ hash }));
  console.log(`i18n merged (${LANGS.join(', ')}) — hash=${hash}`);
}

main().catch(err => {
  console.error(err);
  process.exit(1);
});
