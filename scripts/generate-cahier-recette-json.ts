/**
 * Génère pharmaSmart-app/src/main/resources/data/cahier-recette.json à partir de
 * CAHIER_RECETTE (TypeScript, source de vérité éditée par les devs). Le JSON est
 * l'artefact de build lu par le backend pour générer le guide en PDF (bookmarks + TOC).
 *
 * Il fusionne au passage les captures d'écran produites par la campagne Playwright
 * (e2e/captures/captures.json), recopie les images dans les actifs de l'application et dans
 * le classpath de développement du backend.
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
import { createHash } from 'crypto';
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
const CAPTURES_CLASSPATH_DEST = resolve(ROOT, 'pharmaSmart-app/target/classes/static/content/captures');
const PREFIXE_SERVI = 'content/captures';

interface CaptureBrute {
  scenarioId: string;
  ordre: number;
  fichier: string;
  legende: string;
}

/** Nombre d'images écartées parce qu'elles répétaient l'écran de l'étape précédente. */
let nbFusionnees = 0;

/**
 * Fusionne les écrans identiques de deux étapes CONSÉCUTIVES.
 *
 * Certains gestes ne changent rien à l'écran (cocher une case déjà cochée, ouvrir un menu déjà
 * ouvert, valider une saisie sans retour visuel immédiat) : la campagne produit alors deux
 * images au pixel près identiques, que le lecteur croit devoir comparer. Seule la première est
 * conservée ; l'étape suivante reste affichée, sans illustration redondante.
 *
 * Deux garde-fous, volontaires :
 * - la comparaison porte sur l'empreinte de l'image, pas sur une ressemblance : deux écrans qui
 *   diffèrent d'une ligne de tableau restent deux images ;
 * - seules des étapes voisines sont fusionnées. Un écran qui réapparaît trois étapes plus loin
 *   est de nouveau illustré, parce que le lecteur, lui, a tourné la page.
 */
function fusionnerEcransIdentiques(captures: CaptureEcran[]): CaptureEcran[] {
  const retenues: CaptureEcran[] = [];
  let precedente: CaptureEcran | null = null;

  for (const capture of captures) {
    const memeEcranQueLEtapePrecedente =
      precedente !== null &&
      capture.ordre === precedente.ordre + 1 &&
      !!capture.empreinte &&
      capture.empreinte === precedente.empreinte;

    if (!memeEcranQueLEtapePrecedente) {
      retenues.push(capture);
    } else {
      nbFusionnees++;
    }
    // La référence suit la suite des étapes, pas celle des images retenues : trois étapes
    // identiques d'affilée ne laissent qu'une seule image, et non une sur deux.
    precedente = capture;
  }
  return retenues;
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
      .map<CaptureEcran>(c => {
        const image = readFileSync(join(CAPTURES_SRC, c.fichier));
        return {
          ordre: c.ordre,
          fichier: `${PREFIXE_SERVI}/${c.fichier}`,
          legende: c.legende,
          empreinte: createHash('sha256').update(image).digest('hex'),
        };
      });

    if (presentes.length > 0) {
      parScenario.set(scenarioId, fusionnerEcransIdentiques(presentes));
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
function miroirImages(destination: string): void {
  rmSync(destination, { recursive: true, force: true });
  if (!existsSync(CAPTURES_SRC)) {
    return;
  }
  mkdirSync(destination, { recursive: true });
  cpSync(CAPTURES_SRC, destination, {
    recursive: true,
    // L'index reste côté e2e : ce dossier ne sert qu'à servir les images.
    filter: src => !src.endsWith('captures.json'),
  });
}

/**
 * Index servi au guide affiché à l'écran.
 *
 * Le composant Angular lit `CAHIER_RECETTE` en mémoire, où les captures n'existent pas — elles
 * ne sont fusionnées que dans le JSON livré au backend. Sans cet index, le guide consulté dans
 * l'application n'afficherait aucune image alors que le PDF en est illustré. Il porte exactement
 * les mêmes entrées que le JSON, fusion des écrans répétés comprise.
 *
 * Réduit au strict nécessaire — le rang de l'étape et le chemin de l'image. La légende est le
 * texte de l'étape, que le composant a déjà, et l'empreinte n'a servi qu'à la fusion : les
 * recopier ferait plus que tripler le poids d'un fichier chargé à l'ouverture du guide.
 */
function ecrireIndexServi(destination: string, captures: Map<string, CaptureEcran[]>): void {
  if (!existsSync(destination)) {
    return;
  }
  const allege = Object.fromEntries(
    [...captures].map(([scenarioId, liste]) => [
      scenarioId,
      liste.map(({ ordre, fichier }) => ({ ordre, fichier })),
    ]),
  );
  writeFileSync(join(destination, 'index.json'), JSON.stringify(allege) + '\n', 'utf8');
}

const captures = lireCaptures();
const modules = avecCaptures(CAHIER_RECETTE, captures);

// Le premier miroir est servi par Angular et intégré aux builds frontend. Le second rend les
// images immédiatement disponibles au backend déjà lancé depuis l'IDE : sans lui, le JSON était
// bien actualisé dans target/classes mais le PDF ignorait silencieusement toutes les images.
miroirImages(CAPTURES_DEST);
miroirImages(CAPTURES_CLASSPATH_DEST);
// Après le miroir, qui vide le dossier : l'index y serait sinon effacé aussitôt écrit.
ecrireIndexServi(CAPTURES_DEST, captures);
ecrireIndexServi(CAPTURES_CLASSPATH_DEST, captures);

mkdirSync(dirname(OUT_PATH), { recursive: true });
writeFileSync(OUT_PATH, JSON.stringify(modules, null, 2) + '\n', 'utf8');

const nbImages = [...captures.values()].reduce((total, liste) => total + liste.length, 0);
// eslint-disable-next-line no-console
console.log(
  `cahier-recette.json généré (${modules.length} modules, ` +
    `${captures.size} scénario(s) illustré(s), ${nbImages} image(s), ` +
    `${nbFusionnees} écran(s) répété(s) fusionné(s)) -> ${OUT_PATH}`,
);
