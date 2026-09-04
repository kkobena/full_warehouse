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
  MARQUEURS_ACTIONS,
  MARGE_CADRAGE,
  PROJET_CAPTURES,
} from './config';
import { resoudreScenario, type ScenarioLocalise } from './cahier-recette';

const CLE_INTERACTIONS = '__pharmaSmartCaptureInteractions';
const ID_CALQUE_ACTIONS = '__pharmaSmartActionMarkers';

/**
 * Observe les gestes réellement envoyés à la page par Playwright. Les coordonnées sont prises
 * AVANT que le geste ne ferme un popover, déplace un élément ou ouvre une modale.
 */
function installerObservationActions(): void {
  const cleInteractions = '__pharmaSmartCaptureInteractions';
  const fenetre = window as typeof window & {
    __pharmaSmartCaptureInteractions?: Array<{ x: number; y: number; width: number; height: number; action: string; label: string }>;
    __pharmaSmartActionObserverInstalled?: boolean;
  };
  if (fenetre.__pharmaSmartActionObserverInstalled) {
    return;
  }
  fenetre.__pharmaSmartActionObserverInstalled = true;
  fenetre[cleInteractions] = [];

  const libelle = (element: HTMLElement): string => {
    const explicit = element.getAttribute('aria-label') ?? element.getAttribute('title') ?? element.getAttribute('placeholder');
    const texte = explicit ?? element.innerText ?? element.getAttribute('value') ?? element.tagName.toLowerCase();
    return texte.replace(/\s+/g, ' ').trim().slice(0, 60) || element.tagName.toLowerCase();
  };

  const memoriser = (event: Event, action: string): void => {
    const cible = event.target instanceof Element ? event.target.closest('button, a, input, textarea, select, [role="button"], [role="option"], [contenteditable="true"]') : null;
    if (!(cible instanceof HTMLElement)) {
      return;
    }
    const boite = cible.getBoundingClientRect();
    if (boite.width < 2 || boite.height < 2) {
      return;
    }
    const interactions = (fenetre[cleInteractions] ??= []);
    const precedente = interactions.at(-1);
    const label = libelle(cible);
    // `fill()` peut émettre plusieurs événements input : une seule annotation suffit.
    if (precedente?.action === action && precedente.label === label && Math.abs(precedente.x - boite.x) < 2 && Math.abs(precedente.y - boite.y) < 2) {
      return;
    }
    interactions.push({ x: boite.x, y: boite.y, width: boite.width, height: boite.height, action, label });
  };

  document.addEventListener('pointerdown', event => memoriser(event, 'Cliquer'), true);
  document.addEventListener('input', event => memoriser(event, 'Saisir'), true);
  document.addEventListener('change', event => memoriser(event, 'Sélectionner'), true);
  document.addEventListener('keydown', event => {
    if (event.key === 'Enter' || event.key === ' ') {
      memoriser(event, event.key === 'Enter' ? 'Valider avec Entrée' : 'Activer');
    }
  }, true);
}

async function reinitialiserActions(page: Page): Promise<void> {
  await page.evaluate((cle): void => {
    (window as typeof window & Record<string, unknown>)[cle] = [];
    document.getElementById('__pharmaSmartActionMarkers')?.remove();
  }, CLE_INTERACTIONS).catch((): undefined => undefined);
}

/** Ajoute un calque temporaire, exclusivement présent pendant page.screenshot(). */
async function afficherMarqueursActions(page: Page): Promise<void> {
  await page.evaluate(({ cle, id }) => {
    const interactions = (window as typeof window & Record<string, unknown>)[cle] as
      | Array<{ x: number; y: number; width: number; height: number; action: string; label: string }>
      | undefined;
    if (!interactions?.length) {
      return;
    }
    document.getElementById(id)?.remove();
    const namespace = 'http://www.w3.org/2000/svg';
    const calque = document.createElementNS(namespace, 'svg');
    calque.id = id;
    calque.setAttribute('width', '100%');
    calque.setAttribute('height', '100%');
    calque.setAttribute('aria-hidden', 'true');
    calque.style.cssText = 'position:fixed;inset:0;z-index:2147483647;pointer-events:none;';

    const tracer = (x1: number, y1: number, x2: number, y2: number, couleur: string, largeur: number): void => {
      const ligne = document.createElementNS(namespace, 'line');
      ligne.setAttribute('x1', String(x1));
      ligne.setAttribute('y1', String(y1));
      ligne.setAttribute('x2', String(x2));
      ligne.setAttribute('y2', String(y2));
      ligne.setAttribute('stroke', couleur);
      ligne.setAttribute('stroke-width', String(largeur));
      ligne.setAttribute('stroke-linecap', 'round');
      calque.appendChild(ligne);
    };

    interactions.forEach(interaction => {
      // La pointe arrive juste à l'intérieur du coin du contrôle. Par défaut la flèche vient
      // d'en haut à gauche ; près du bord supérieur elle vient d'en bas à gauche.
      const finX = interaction.x + Math.min(18, interaction.width / 3);
      const finY = interaction.y + Math.min(14, interaction.height / 3);
      const depuisLeBas = finY < 55;
      const debutX = Math.max(8, finX - 42);
      const debutY = depuisLeBas ? finY + 42 : finY - 42;
      const angle = Math.atan2(finY - debutY, finX - debutX);
      const longueurPointe = 13;
      const ouverture = Math.PI / 7;
      const p1x = finX - longueurPointe * Math.cos(angle - ouverture);
      const p1y = finY - longueurPointe * Math.sin(angle - ouverture);
      const p2x = finX - longueurPointe * Math.cos(angle + ouverture);
      const p2y = finY - longueurPointe * Math.sin(angle + ouverture);

      // Liseré blanc sous la flèche : elle reste visible sur un bouton clair comme foncé.
      tracer(debutX, debutY, finX, finY, 'white', 7);
      tracer(debutX, debutY, finX, finY, '#e84b0f', 3.5);

      const pointe = document.createElementNS(namespace, 'polygon');
      pointe.setAttribute('points', `${finX},${finY} ${p1x},${p1y} ${p2x},${p2y}`);
      pointe.setAttribute('fill', '#e84b0f');
      pointe.setAttribute('stroke', 'white');
      pointe.setAttribute('stroke-width', '2');
      pointe.setAttribute('stroke-linejoin', 'round');
      calque.appendChild(pointe);
    });
    document.body.appendChild(calque);
  }, { cle: CLE_INTERACTIONS, id: ID_CALQUE_ACTIONS });
}

async function masquerMarqueursActions(page: Page): Promise<void> {
  await page.evaluate((id): void => {
    document.getElementById(id)?.remove();
  }, ID_CALQUE_ACTIONS).catch((): undefined => undefined);
}

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
    .locator('#main-content, main, [role="main"]')
    .first()
    .boundingBox()
    .catch((): null => null);

  const { basContenuSignificatif, elementMax } = await page.evaluate((hauteurFenetre): { basContenuSignificatif: number; elementMax: string } => {
    const selecteurs = [
      '#main-content h1', '#main-content h2', '#main-content h3', '#main-content h4', '#main-content h5', '#main-content h6',
      '#main-content p', '#main-content li', '#main-content td', '#main-content th', '#main-content tr',
      '#main-content button', '#main-content input', '#main-content textarea', '#main-content select', '#main-content label',
      '#main-content a', '#main-content span', '#main-content strong', '#main-content b', '#main-content small',
      '#main-content img', '#main-content svg', '#main-content canvas',
      '#main-content [role="button"]', '#main-content [role="option"]', '#main-content [role="alert"]', '#main-content [role="gridcell"]',
      '#main-content [role="tab"]',
      '#main-content .ag-row', '#main-content .ag-header',
      '#main-content .grid-caption', '#main-content .badge', '#main-content app-badge', '#main-content app-button',
      '#main-content .alert', '#main-content .toast',
      '.modal-content', '.popover', 'ngb-popover-window', '.dropdown-menu.show',
    ].join(',');
    let bas = 0;
    let maxEl = '';
    for (const element of document.querySelectorAll(selecteurs)) {
      if (!(element instanceof HTMLElement || element instanceof SVGElement)) {
        continue;
      }
      const style = getComputedStyle(element);
      const rectangle = element.getBoundingClientRect();
      const visible = style.display !== 'none' && style.visibility !== 'hidden' && Number(style.opacity) !== 0
        && rectangle.width > 1 && rectangle.height > 1 && rectangle.bottom > 0 && rectangle.top < hauteurFenetre;
      if (visible && rectangle.height < hauteurFenetre * 0.9) {
        if (rectangle.bottom > bas) {
          bas = Math.min(hauteurFenetre, rectangle.bottom);
          maxEl = `${element.tagName}.${element.className} (top=${Math.round(rectangle.top)}, bottom=${Math.round(rectangle.bottom)}, h=${Math.round(rectangle.height)}, text=${(element.textContent || '').trim().slice(0, 30)})`;
        }
      }
    }
    return { basContenuSignificatif: Math.ceil(bas), elementMax: maxEl };
  }, fenetre.height);
  console.log(`[CADRAGE] max element: ${elementMax}`);

  const basStructurel = boite ? Math.ceil(boite.y + boite.height) : fenetre.height;
  // Les éléments sémantiques visibles donnent la borne la plus fidèle du contenu réel,
  // évitant de capturer le fond d'écran ou l'espace vide sous les grilles.
  const basUtile = basContenuSignificatif > 0
    ? basContenuSignificatif
    : basStructurel;
  const hauteur = Math.min(fenetre.height, Math.max(1, basUtile + MARGE_CADRAGE));
  // Seule une image déjà ajustée au pixel près reste en pleine hauteur. Une économie même
  // modeste devient significative lorsqu'elle est répétée dans plusieurs centaines de vues.
  if (hauteur >= fenetre.height) {
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

    if (capturer && MARQUEURS_ACTIONS) {
      await page.addInitScript(installerObservationActions);
      await page.evaluate(installerObservationActions);
    }

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
      if (capturer && MARQUEURS_ACTIONS) {
        await reinitialiserActions(page);
      }
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
      if (MARQUEURS_ACTIONS) {
        await afficherMarqueursActions(page);
      }
      try {
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
      } finally {
        await masquerMarqueursActions(page);
      }

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
