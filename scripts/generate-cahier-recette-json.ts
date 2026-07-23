/**
 * Génère pharmaSmart-app/src/main/resources/data/cahier-recette.json à partir de
 * CAHIER_RECETTE (TypeScript, source de vérité éditée par les devs). Le JSON est
 * l'artefact de build lu par le backend pour générer le guide en PDF (bookmarks + TOC).
 *
 * Lancé via `npm run generate:cahier-recette`, et automatiquement à chaque build Maven
 * (exec-maven-plugin, phase generate-resources) — cf. pharmaSmart-app/pom.xml.
 */
import { mkdirSync, writeFileSync } from 'fs';
import { dirname, resolve } from 'path';
import { fileURLToPath } from 'url';
import { CAHIER_RECETTE } from '../pharmaSmart-app/src/main/webapp/app/features/cahier-recette/cahier-recette.model';

const __dirname = dirname(fileURLToPath(import.meta.url));
const ROOT = resolve(__dirname, '..');
const OUT_PATH = resolve(ROOT, 'pharmaSmart-app/src/main/resources/data/cahier-recette.json');

mkdirSync(dirname(OUT_PATH), { recursive: true });
writeFileSync(OUT_PATH, JSON.stringify(CAHIER_RECETTE, null, 2) + '\n', 'utf8');

// eslint-disable-next-line no-console
console.log(`cahier-recette.json généré (${CAHIER_RECETTE.length} modules) -> ${OUT_PATH}`);
