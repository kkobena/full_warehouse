/**
 * Génère pharmaSmart-app/src/main/resources/data/cahier-recette.json à partir de
 * CAHIER_RECETTE (TypeScript, source de vérité éditée par les devs). Le JSON est
 * l'artefact de build lu par le backend pour générer le guide en PDF (bookmarks + TOC).
 *
 * Il fusionne au passage les captures d'écran produites par la campagne Playwright
 * (e2e/captures/captures.json) et recopie les images dans les actifs de l'application.
 * Les deux sources restent séparées à dessein : le modèle est écrit à la main, l'index des
 * captures est écrit par la machine — les mélanger dans un même fichier condamnerait le
 * modèle à un diff à chaque exécution de la campagne.
 *
 * Sans campagne (index absent), le comportement est exactement celui d'avant : le JSON est
 * produit tel quel, sans champ `captures`.
 *
 * Lancé via `npm run generate:cahier-recette`, et automatiquement à chaque build Maven
 * (exec-maven-plugin, phase generate-resources) — cf. pharmaSmart-app/pom.xml.
 */
import { cpSync, existsSync, mkdirSync, readFileSync, rmSync, writeFileSync } from 'fs';
import { dirname, join, resolve } from 'path';
import { fileURLToPath } from 'url';
import { CAHIER_RECETTE, type CaptureEcran, type ModuleRecette } from '../pharmaSmart-app/src/main/webapp/app/features/cahier-recette/cahier-recette.model';

const __dirname = dirname(fileURLToPath(import.meta.url));
const ROOT = resolve(__dirname, '..');
const OUT_PATH = resolve(ROOT, 'pharmaSmart-app/src/main/resources/data/cahier-recette.json');

/** Index produit par le reporter Playwright ; absent tant qu'aucune campagne n'a tourné. */
const CAPTURES_SRC = resolve(ROOT, 'e2e/captures');
const INDEX_CAPTURES = join(CAPTURES_SRC, 'captures.json');

/**
 * Destination des images. `content/` est déjà déclaré dans les assets d'angular.json : les
 * fichiers y sont servis à l'identique par le serveur de développement (port 4200) et par le
 * build de production, et se retrouvent donc aussi dans le classpath du backend sous
 * `static/content/...`, d'où le PDF les lit.
 */
const CAPTURES_DEST = resolve(ROOT, 'pharmaSmart-app/src/main/webapp/content/captures');
const PREFIXE_SERVI = 'content/captures';

interface CaptureBrute {
  scenarioId: string;
  ordre: number;
  fichier: string;
  legende: string;
}

function lireCaptures(): Map<string, CaptureEcran[]> {
  const parScenario = new Map<string, CaptureEcran[]>();
  if (!existsSync(INDEX_CAPTURES)) {
    return parScenario;
  }

  const index = JSON.parse(readFileSync(INDEX_CAPTURES, 'utf8')) as Record<string, CaptureBrute[]>;

  for (const [scenarioId, captures] of Object.entries(index)) {
    // Une entrée dont l'image a disparu (dossier nettoyé, campagne partielle) produirait une
    // image cassée dans le guide : on l'écarte plutôt que de la propager.
    const presentes = captures
      .filter(c => existsSync(join(CAPTURES_SRC, c.fichier)))
      .sort((a, b) => a.ordre - b.ordre)
      .map<CaptureEcran>(c => ({
        ordre: c.ordre,
        fichier: `${PREFIXE_SERVI}/${c.fichier}`,
        legende: c.legende,
      }));

    if (presentes.length > 0) {
      parScenario.set(scenarioId, presentes);
    }
  }
  return parScenario;
}

function avecCaptures(modules: ModuleRecette[], captures: Map<string, CaptureEcran[]>): ModuleRecette[] {
  if (captures.size === 0) {
    return modules;
  }
  return modules.map(m => ({
    ...m,
    fonctionnalites: m.fonctionnalites.map(f => ({
      ...f,
      scenarios: f.scenarios.map(s => {
        const trouvees = captures.get(s.id);
        return trouvees ? { ...s, captures: trouvees } : s;
      }),
    })),
  }));
}

/**
 * Recopie les images dans les actifs. Le dossier de destination est un MIROIR strict de
 * `e2e/captures`, jamais un cumul : il est vidé à chaque génération, y compris lorsqu'aucune
 * campagne n'a tourné.
 *
 * Conséquence assumée, corollaire du choix de ne pas versionner les captures (§8.1 du plan) :
 * un build sans campagne préalable produit un guide sans illustrations. C'est préférable à
 * l'inverse — des images orphelines, laissées par une campagne précédente, illustreraient des
 * scénarios que le modèle ne décrit plus.
 */
function miroirImages(): void {
  rmSync(CAPTURES_DEST, { recursive: true, force: true });
  if (!existsSync(CAPTURES_SRC)) {
    return;
  }
  mkdirSync(CAPTURES_DEST, { recursive: true });
  cpSync(CAPTURES_SRC, CAPTURES_DEST, {
    recursive: true,
    // L'index reste côté e2e : ce dossier ne sert qu'à servir les images.
    filter: src => !src.endsWith('captures.json'),
  });
}

const captures = lireCaptures();
const modules = avecCaptures(CAHIER_RECETTE, captures);

miroirImages();

mkdirSync(dirname(OUT_PATH), { recursive: true });
writeFileSync(OUT_PATH, JSON.stringify(modules, null, 2) + '\n', 'utf8');

const nbImages = [...captures.values()].reduce((total, liste) => total + liste.length, 0);
// eslint-disable-next-line no-console
console.log(
  `cahier-recette.json généré (${modules.length} modules, ` +
    `${captures.size} scénario(s) illustré(s), ${nbImages} image(s)) -> ${OUT_PATH}`,
);
