/**
 * La fixture `scenario()` : le liage entre le cahier de recette, le parcours Playwright et les
 * captures du manuel.
 *
 * Un parcours ne redécrit pas ses étapes, il se rattache à un identifiant du modèle :
 *
 *     scenario('VTE-01', async ({ etape, page }) => {
 *       await etape(1, async () => { ... });
 *       await etape(2, async () => { ... });
 *       await expect(...).toBeVisible();   // cf. §3.1 du plan
 *     });
 *
 * Ce que cela garantit, et qui justifie l'indirection :
 *   1. l'identifiant existe dans le modèle — sinon la campagne s'arrête au chargement ;
 *   2. toutes les étapes documentées sont parcourues — sinon le test échoue ;
 *   3. la légende de l'image est le texte de l'étape, pris dans le modèle : le manuel ne peut
 *      pas décrire autre chose que ce qui a été exécuté.
 */
import { test, type Page } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { dirname, join, posix } from 'node:path';
import {
  CADRAGE_ESSENTIEL,
  CAPTURE_FORCEE,
  DOSSIER_CAPTURES,
  IMAGE,
  MARGE_CADRAGE,
  PROJET_CAPTURES,
} from './config';
import { resoudreScenario, type ScenarioLocalise } from './cahier-recette';

/**
 * Ramène la hauteur de l'image au bas du contenu applicatif.
 *
 * `#main-content` est le conteneur de l'écran routé, commun à toute l'application : il
 * s'ajuste à son contenu, alors que le fond décoratif, lui, occupe toute la fenêtre. Cadrer
 * dessus supprime le vide sans rien retirer de la mise en page.
 *
 * Retourne `undefined` — donc capture pleine fenêtre — si le conteneur est introuvable, si le
 * contenu remplit déjà la fenêtre, ou si le cadrage est désactivé. Aucun de ces cas n'est une
 * anomalie : mieux vaut une image trop grande qu'une image tronquée.
 */
async function cadrer(page: Page): Promise<{ x: number; y: number; width: number; height: number } | undefined> {
  if (!CADRAGE_ESSENTIEL || IMAGE.pleinePage) {
    return undefined;
  }
  const fenetre = page.viewportSize();
  if (!fenetre) {
    return undefined;
  }

  const boite = await page
    .locator('#main-content')
    .first()
    .boundingBox()
    .catch((): null => null);
  if (!boite) {
    return undefined;
  }

  const hauteur = Math.min(fenetre.height, Math.ceil(boite.y + boite.height) + MARGE_CADRAGE);
  // En deçà d'une centaine de pixels d'économie, le recadrage ne vaut pas l'écart de format
  // entre les images du manuel.
  if (hauteur >= fenetre.height - 100) {
    return undefined;
  }
  return { x: 0, y: 0, width: fenetre.width, height: hauteur };
}

/** Une image du manuel, telle que la relit le reporter puis le générateur du guide. */
export interface CaptureIndexee {
  scenarioId: string;
  ordre: number;
  /** Chemin relatif au dossier des captures, en séparateurs POSIX : « VTE-01/etape-1.jpg ». */
  fichier: string;
  legende: string;
}

/** Préfixe des pièces jointes lues par captures-reporter.ts. */
export const PREFIXE_PIECE_JOINTE = 'capture:';

export interface Etape {
  /** Exécute l'action de l'étape `numero`, puis capture l'écran si le mode capture est actif. */
  (numero: number, action: () => Promise<void>): Promise<void>;
  /**
   * Déclare l'étape `numero` couverte sans la parcourir : elle sort du champ de
   * l'automatisation (geste matériel, imprimante thermique, action hors application).
   * Sans cette échappatoire, le contrôle de complétude interdirait tout scénario
   * partiellement automatisable — et l'on serait tenté de désactiver le contrôle lui-même.
   */
  horsPortee(numero: number, motif: string): void;
}

export interface ContexteScenario {
  page: Page;
  /** Le scénario du modèle, son module et sa fonctionnalité. */
  info: ScenarioLocalise;
  etape: Etape;
  /**
   * Lève le garde-fou « écran sain » pour ce scénario, quand une erreur serveur ou une
   * exception de page fait partie de ce que l'on veut montrer (message d'erreur métier,
   * cas de refus). À n'employer qu'avec un motif explicite.
   */
  tolererErreurs(motif: string): void;
}

export function scenario(id: string, corps: (ctx: ContexteScenario) => Promise<void>): void {
  // Résolu ici, à la lecture du fichier : une faute de frappe dans l'identifiant arrête la
  // campagne avant même d'ouvrir un navigateur.
  const info = resoudreScenario(id);
  const nbEtapes = info.scenario.etapes.length;

  test(`${id} — ${info.scenario.titre}`, async ({ page }, testInfo) => {
    const couvertes = new Map<number, 'parcourue' | 'hors-portee'>();
    const anomalies: string[] = [];
    let erreursTolerees: string | null = null;
    const capturer = CAPTURE_FORCEE || testInfo.project.name === PROJET_CAPTURES;

    // Garde-fou de §3.1 : un parcours sans assertion photographie sans broncher une page
    // d'erreur. Ces deux écoutes coûtent presque rien et couvrent le cas le plus courant —
    // une exception JavaScript ou une réponse 5xx pendant le parcours.
    page.on('pageerror', erreur => anomalies.push(`exception de page : ${erreur.message}`));
    page.on('response', reponse => {
      if (reponse.status() >= 500) {
        anomalies.push(`réponse ${reponse.status()} sur ${reponse.url()}`);
      }
    });

    const verifierNumero = (numero: number): void => {
      if (!Number.isInteger(numero) || numero < 1 || numero > nbEtapes) {
        throw new Error(
          `${id} : étape ${numero} hors du modèle, qui en décrit ${nbEtapes}. ` +
            `Ajouter l'étape dans cahier-recette.model.ts avant de la parcourir.`,
        );
      }
      if (couvertes.has(numero)) {
        throw new Error(`${id} : étape ${numero} déclarée deux fois.`);
      }
    };

    const etape = (async (numero: number, action: () => Promise<void>): Promise<void> => {
      verifierNumero(numero);
      await action();
      couvertes.set(numero, 'parcourue');

      if (!capturer) {
        return;
      }

      // Le curseur reste là où le dernier clic l'a laissé : le bouton garde son état
      // survolé et son infobulle s'affiche par-dessus l'écran. On l'écarte dans un coin
      // hors de la carte applicative avant de déclencher la capture.
      const fenetre = page.viewportSize();
      if (fenetre) {
        await page.mouse.move(fenetre.width - 1, fenetre.height - 1);
      }

      const relatif = posix.join(id, `etape-${numero}.jpg`);
      const absolu = join(DOSSIER_CAPTURES, id, `etape-${numero}.jpg`);
      mkdirSync(dirname(absolu), { recursive: true });
      await page.screenshot({
        path: absolu,
        type: IMAGE.type,
        quality: IMAGE.quality,
        fullPage: IMAGE.pleinePage,
        clip: await cadrer(page),
        // `animations` fige les transitions restantes ; `caret` masque le curseur clignotant,
        // qui apparaîtrait une image sur deux dans un champ de saisie.
        animations: 'disabled',
        caret: 'hide',
      });

      const capture: CaptureIndexee = {
        scenarioId: id,
        ordre: numero,
        fichier: relatif,
        legende: info.scenario.etapes[numero - 1],
      };
      await testInfo.attach(`${PREFIXE_PIECE_JOINTE}${id}:${numero}`, {
        contentType: 'application/json',
        body: JSON.stringify(capture),
      });
    }) as Etape;

    etape.horsPortee = (numero: number, motif: string): void => {
      verifierNumero(numero);
      couvertes.set(numero, 'hors-portee');
      testInfo.annotations.push({ type: 'hors-portee', description: `étape ${numero} : ${motif}` });
    };

    const tolererErreurs = (motif: string): void => {
      erreursTolerees = motif;
      testInfo.annotations.push({ type: 'erreurs-tolerees', description: motif });
    };

    await corps({ page, info, etape, tolererErreurs });

    const manquantes = Array.from({ length: nbEtapes }, (_, i) => i + 1).filter(n => !couvertes.has(n));
    if (manquantes.length > 0) {
      throw new Error(
        `${id} : étape(s) ${manquantes.join(', ')} du modèle non parcourue(s). ` +
          `Compléter le parcours, ou les déclarer via etape.horsPortee(n, motif).`,
      );
    }

    if (anomalies.length > 0 && erreursTolerees === null) {
      throw new Error(
        `${id} : l'écran n'était pas sain pendant le parcours — les captures seraient trompeuses.\n` +
          anomalies.map(a => `  - ${a}`).join('\n') +
          `\nSi ces erreurs font partie du scénario, appeler tolererErreurs('<motif>').`,
      );
    }
  });
}
