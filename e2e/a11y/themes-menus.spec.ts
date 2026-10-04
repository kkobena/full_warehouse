/**
 * Barres de menu (barre horizontale, rail) et menus déroulants sous chaque thème de couleur, au regard de WCAG 2.2 AA
 * (docs/PLAN-THEMES-APPLICATION.md, phase 4). Complète le calcul de `theme-contraste.spec.ts`, qui ne voit que des couleurs
 * isolées, et `themes-chrome.spec.ts`, où axe ne sait pas lire un texte posé sur un dégradé.
 *
 * Ce qui est mesuré, dans le navigateur, sur ce qui est réellement rendu :
 *  - 1.4.3 / 1.4.11 contraste du texte (4,5:1) et des icônes (3:1) **sur les pixels du fond** (le texte est rendu
 *    transparent, la page photographiée, puis chaque élément comparé à tous les pixels derrière lui) — au repos, au survol,
 *    au focus, pour l'entrée active, dans le menu déroulant ouvert et sur sa ligne survolée ;
 *  - 2.4.7 / 1.4.11 visibilité du focus clavier : l'élément focalisé est photographié avec et sans focus ; le signe ajouté doit
 *    exister et contraster à 3:1 avec ce qu'il recouvre ;
 *  - 1.4.1 l'entrée active se distingue par autre chose que la seule couleur ;
 *  - 2.5.8 taille des cibles : 24 × 24 px au moins ;
 *  - 2.1.1 / 2.1.2 clavier : Entrée ouvre le menu déroulant, Échap le ferme et rend le focus au déclencheur ;
 *  - 4.1.2 nom, rôle, valeur, et les autres règles WCAG 2 A/AA d'axe, sur les barres et le menu ouvert.
 *
 * Principe d'acceptation : « Actuel » est la référence ; **un thème échoue seulement pour un défaut absent sous « Actuel »**.
 * Les défauts d'« Actuel » sont consignés (rapport JSON), ils ne dépendent pas des thèmes.
 *
 * Lecture seule. Résultats : `target/a11y-themes/menus.json`.
 */
import AxeBuilder from '@axe-core/playwright';
import { expect, test, type Page } from '@playwright/test';
import { mkdirSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { RACINE } from '../src/config';

const DOSSIER = join(RACINE, 'target', 'a11y-themes');
const THEMES = ['actuel', 'comptant', 'assurance', 'prevente-comptant', 'prevente-carnet'] as const;
const DISPOSITIONS = ['navbar', 'sidebar'] as const;
const RACINES = { navbar: 'nav.navbar-chrome', sidebar: 'aside.sidebar' } as const;

interface Cible {
  genre: 'texte' | 'icône';
  libelle: string;
  couleur: string;
  opacite: number;
  taille: number;
  gras: boolean;
  x: number;
  y: number;
  l: number;
  h: number;
}

/** Coupe les transitions : une couleur mesurée à mi-course n'est pas celle de l'état. */
async function figer(page: Page): Promise<void> {
  await page.addStyleTag({ content: '*, *::before, *::after { transition: none !important; animation: none !important; }' });
}

/**
 * Contraste réel du texte et des icônes sous `portee`. Renvoie les défauts sous la forme « genre « libellé » : ratio ».
 * `portee` : sélecteur CSS de la zone mesurée (la barre entière, un seul item, le menu déroulant…).
 */
async function mesurerContraste(page: Page, portee: string, etat: string): Promise<string[]> {
  const cibles: Cible[] = await page.evaluate(zone => {
    const racines = [...document.querySelectorAll(zone)];
    const opaciteCumulee = (e: Element): number => {
      let o = 1;
      for (let n: Element | null = e; n && n !== document.body; n = n.parentElement) {
        o *= Number(getComputedStyle(n).opacity);
      }
      return o;
    };
    const liste: Cible[] = [];
    const ajouter = (e: Element, r: DOMRect, libelle: string, genre: Cible['genre']): void => {
      const cs = getComputedStyle(e);
      if (cs.visibility === 'hidden' || cs.display === 'none' || r.width < 2 || r.height < 2) {
        return;
      }
      // Texte en dégradé (`background-clip: text`) : le fond EST le texte, la mesure par pixels n'a pas de sens ici.
      if (cs.backgroundClip === 'text' || cs.webkitBackgroundClip === 'text') {
        return;
      }
      if (r.bottom < 0 || r.top > innerHeight || r.right < 0 || r.left > innerWidth) {
        return;
      }
      liste.push({ genre, libelle, couleur: cs.color, opacite: opaciteCumulee(e), taille: parseFloat(cs.fontSize), gras: Number(cs.fontWeight) >= 700, x: r.x, y: r.y, l: r.width, h: r.height });
    };
    for (const racine of racines) {
      const parcours = document.createTreeWalker(racine, NodeFilter.SHOW_TEXT);
      while (parcours.nextNode()) {
        const noeud = parcours.currentNode;
        if (!noeud.textContent?.trim() || !noeud.parentElement) {
          continue;
        }
        const plage = document.createRange();
        plage.selectNodeContents(noeud);
        ajouter(noeud.parentElement, plage.getBoundingClientRect(), noeud.textContent.trim().slice(0, 30), 'texte');
      }
      racine.querySelectorAll('svg, i.pi').forEach(i => {
        const nom = i.closest('[aria-label]')?.getAttribute('aria-label') ?? i.closest('a, button')?.textContent?.trim().slice(0, 24) ?? 'icône';
        ajouter(i, i.getBoundingClientRect(), nom || 'icône', 'icône');
      });
    }
    return liste;
  }, portee);
  if (cibles.length === 0) {
    return [`${etat} : aucune cible dans « ${portee} »`];
  }

  const style = await page.addStyleTag({
    content: `${portee} *{color:transparent!important;-webkit-text-fill-color:transparent!important;text-shadow:none!important} ${portee} svg, ${portee} i.pi{opacity:0!important}`,
  });
  await page.waitForTimeout(150);
  const photo = `data:image/png;base64,${(await page.screenshot()).toString('base64')}`;
  await style.evaluate((e: Element): void => e.remove());

  return page.evaluate(
    async ([source, liste, contexte]) => {
      const image = new Image();
      image.src = source as string;
      await image.decode();
      const toile = document.createElement('canvas');
      toile.width = image.width;
      toile.height = image.height;
      const g = toile.getContext('2d') as CanvasRenderingContext2D;
      g.drawImage(image, 0, 0);
      const lum = (c: number[]): number => {
        const f = (v: number): number => (v / 255 <= 0.03928 ? v / 255 / 12.92 : Math.pow((v / 255 + 0.055) / 1.055, 2.4));
        return 0.2126 * f(c[0]) + 0.7152 * f(c[1]) + 0.0722 * f(c[2]);
      };
      const rapport = (a: number[], b: number[]): number => {
        const x = lum(a);
        const y = lum(b);
        return (Math.max(x, y) + 0.05) / (Math.min(x, y) + 0.05);
      };
      const lire = (couleur: string): { rgb: number[]; alpha: number } => {
        const n = (couleur.match(/-?[\d.]+(?:e-?\d+)?/g) ?? []).map(Number);
        const srgb = couleur.startsWith('color(');
        const rgb = n.slice(0, 3).map(v => (srgb ? v * 255 : v));
        return { rgb, alpha: n.length > 3 ? n[3] : 1 };
      };
      const k = image.width / innerWidth;
      const defauts: string[] = [];
      for (const e of liste as Cible[]) {
        const { rgb, alpha: a0 } = lire(e.couleur);
        const alpha = a0 * e.opacite;
        // Zone resserrée : les pixels de bord d'une pastille arrondie sont antialiasés contre la barre derrière elle et ne
        // sont pas le fond du texte (faux positif vu sur « 32 », blanc sur #c2410c = 5,2:1). On garde le cœur de la zone.
        const retrait = e.genre === 'texte' ? 0.18 : 0.1;
        const x0 = Math.max(0, Math.floor((e.x + e.l * retrait) * k));
        const y0 = Math.max(0, Math.floor((e.y + e.h * retrait) * k));
        const largeur = Math.max(1, Math.min(image.width - x0, Math.ceil(e.l * (1 - 2 * retrait) * k)));
        const hauteur = Math.max(1, Math.min(image.height - y0, Math.ceil(e.h * (1 - 2 * retrait) * k)));
        const donnees = g.getImageData(x0, y0, largeur, hauteur).data;
        let pire = 99;
        for (let i = 0; i < donnees.length; i += 4) {
          const fond = [donnees[i], donnees[i + 1], donnees[i + 2]];
          const avant = rgb.map((v, j) => v * alpha + fond[j] * (1 - alpha));
          pire = Math.min(pire, rapport(avant, fond));
        }
        const seuil = e.genre === 'texte' ? (e.taille >= 24 || (e.taille >= 18.66 && e.gras) ? 3 : 4.5) : 3;
        if (pire < seuil) {
          defauts.push(`${contexte} : ${e.genre} « ${e.libelle} » ${pire.toFixed(2)}:1 (seuil ${seuil})`);
        }
      }
      return defauts;
    },
    [photo, cibles, etat] as [string, Cible[], string],
  );
}

/** 2.4.7 / 1.4.11 : le focus doit ajouter un signe visible, à 3:1 de ce qu'il recouvre. */
async function mesurerFocus(page: Page, selecteur: string, nom: string): Promise<string | null> {
  const el = page.locator(selecteur).first();
  const boite = await el.boundingBox();
  if (!boite) {
    return `${nom} : introuvable`;
  }
  const marge = 6;
  const clip = { x: Math.max(0, boite.x - marge), y: Math.max(0, boite.y - marge), width: boite.width + 2 * marge, height: boite.height + 2 * marge };
  await el.evaluate((e: HTMLElement) => e.blur());
  await page.mouse.move(2, 2);
  await page.waitForTimeout(80);
  const avant = `data:image/png;base64,${(await page.screenshot({ clip })).toString('base64')}`;
  await page.keyboard.press('Shift'); // bascule en modalité clavier : :focus-visible s'applique au focus() suivant
  await el.focus();
  await page.waitForTimeout(80);
  const apres = `data:image/png;base64,${(await page.screenshot({ clip })).toString('base64')}`;
  const r = await page.evaluate(async ([a, b]) => {
    const charge = async (src: string): Promise<Uint8ClampedArray> => {
      const im = new Image();
      im.src = src;
      await im.decode();
      const c = document.createElement('canvas');
      c.width = im.width;
      c.height = im.height;
      const g = c.getContext('2d') as CanvasRenderingContext2D;
      g.drawImage(im, 0, 0);
      return g.getImageData(0, 0, im.width, im.height).data;
    };
    const lum = (v: number[]): number => {
      const f = (x: number): number => (x / 255 <= 0.03928 ? x / 255 / 12.92 : Math.pow((x / 255 + 0.055) / 1.055, 2.4));
      return 0.2126 * f(v[0]) + 0.7152 * f(v[1]) + 0.0722 * f(v[2]);
    };
    const A = await charge(a);
    const B = await charge(b);
    let changes = 0;
    let meilleur = 1;
    for (let i = 0; i < A.length; i += 4) {
      const d = Math.abs(A[i] - B[i]) + Math.abs(A[i + 1] - B[i + 1]) + Math.abs(A[i + 2] - B[i + 2]);
      if (d > 30) {
        changes++;
        const x = lum([A[i], A[i + 1], A[i + 2]]);
        const y = lum([B[i], B[i + 1], B[i + 2]]);
        meilleur = Math.max(meilleur, (Math.max(x, y) + 0.05) / (Math.min(x, y) + 0.05));
      }
    }
    return { changes, meilleur };
  }, [avant, apres] as [string, string]);
  if (r.changes < 6) {
    return `focus invisible : ${nom} (${r.changes} pixel(s) modifié(s))`;
  }
  if (r.meilleur < 3) {
    return `focus trop discret : ${nom} (${r.meilleur.toFixed(2)}:1, seuil 3)`;
  }
  return null;
}

/** 1.4.1 : l'entrée active ne se distingue pas par la seule couleur. */
async function indiceActif(page: Page, racine: string): Promise<string | null> {
  return page.evaluate(zone => {
    const r = document.querySelector(zone);
    const actif = r?.querySelector('.router-link-active, li.active > a, a.active, a[aria-current="page"]') as HTMLElement | null;
    if (!actif) {
      return null; // pas d'entrée active sur cet écran : rien à mesurer
    }
    const autre = [...(r?.querySelectorAll('a.nav-link, button.nav-link') ?? [])].find(e => e !== actif && !actif.contains(e) && !e.contains(actif)) as HTMLElement | undefined;
    if (!autre) {
      return null;
    }
    const lire = (e: HTMLElement) => {
      const cs = getComputedStyle(e);
      return [cs.fontWeight, cs.textDecorationLine, cs.boxShadow, cs.borderLeftWidth, cs.borderBottomWidth, cs.backgroundImage, cs.backgroundColor].join('|');
    };
    const li = (e: HTMLElement) => e.closest('li') as HTMLElement | null;
    const a = lire(actif) + '#' + (li(actif) ? lire(li(actif) as HTMLElement) : '');
    const b = lire(autre) + '#' + (li(autre) ? lire(li(autre) as HTMLElement) : '');
    return a === b ? 'entrée active identique aux autres hors couleur du texte (1.4.1)' : null;
  }, racine);
}

async function defautsAxe(page: Page, racines: string[], etat: string): Promise<string[]> {
  let b = new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa']).disableRules(['color-contrast']);
  for (const r of racines) {
    b = b.include(r);
  }
  const res = await b.analyze();
  return res.violations.flatMap(v => v.nodes.map(n => `${etat} : axe ${v.id} — ${n.target.join(' ')}`));
}

async function tailleDesCibles(page: Page, portee: string, etat: string): Promise<string[]> {
  return page.evaluate(
    ([zone, contexte]) => {
      const petits: string[] = [];
      document.querySelectorAll(`${zone} a, ${zone} button, ${zone} [role="menuitem"]`).forEach(e => {
        const r = e.getBoundingClientRect();
        const cs = getComputedStyle(e);
        if (r.width < 2 || r.height < 2 || cs.visibility === 'hidden' || cs.display === 'none') {
          return;
        }
        if (r.width < 24 || r.height < 24) {
          petits.push(`${contexte} : cible ${Math.round(r.width)}×${Math.round(r.height)} px « ${(e.getAttribute('aria-label') ?? e.textContent ?? '').trim().slice(0, 24)} »`);
        }
      });
      return petits;
    },
    [portee, etat] as [string, string],
  );
}

const reference = new Map<string, Set<string>>();

for (const disposition of DISPOSITIONS) {
  for (const theme of THEMES) {
    test(`menus ${disposition} — ${theme}`, async ({ page }) => {
      test.setTimeout(420_000);
      await page.addInitScript(
        ([d, t]) => {
          localStorage.setItem('pharmasmart_layout_mode', d);
          localStorage.setItem('pharmasmart_chrome', t);
          localStorage.setItem('pharmasmart_sidebar_collapsed', 'false');
        },
        [disposition, theme],
      );
      await page.setViewportSize({ width: 1920, height: 1080 });
      await page.goto('/mvt-caisse');
      await page.waitForLoadState('networkidle');
      await figer(page);
      const racine = RACINES[disposition];
      await page.locator(racine).first().waitFor();
      const defauts: string[] = [];

      // — repos : toute la barre
      defauts.push(...(await mesurerContraste(page, racine, 'repos')));
      defauts.push(...(await tailleDesCibles(page, racine, 'repos')));
      const actif = await indiceActif(page, racine);
      if (actif) {
        defauts.push(`actif : ${actif}`);
      }

      // — survol et focus, item par item
      const triggers = page.locator(`${racine} [data-flyout-trigger], ${racine} a.nav-link, ${racine} button.nav-link`);
      const n = Math.min(await triggers.count(), 14);
      for (let i = 0; i < n; i++) {
        const item = triggers.nth(i);
        if (!(await item.isVisible())) {
          continue;
        }
        const nom = ((await item.getAttribute('aria-label')) ?? (await item.textContent()) ?? `item ${i}`).trim().replace(/\s+/g, ' ').slice(0, 26);
        await item.hover();
        await page.waitForTimeout(80);
        await item.evaluate((e: HTMLElement) => {
          document.querySelectorAll('[data-mesure]').forEach(x => x.removeAttribute('data-mesure'));
          (e.closest('li') ?? e).setAttribute('data-mesure', '1');
        });
        defauts.push(...(await mesurerContraste(page, `${racine} [data-mesure]`, `survol « ${nom} »`)));
        await item.evaluate((e: HTMLElement) => (e.closest('li') ?? e).removeAttribute('data-mesure'));
        await page.mouse.move(2, 2);
        const focus = await mesurerFocus(page, `${racine} [data-flyout-trigger], ${racine} a.nav-link, ${racine} button.nav-link >> nth=${i}`, nom);
        if (focus) {
          defauts.push(focus);
        }
        await item.evaluate((e: HTMLElement) => e.blur());
      }

      // — menu déroulant : ouverture au clavier, contraste, survol, fermeture
      const declencheur = page.locator(`${racine} [data-flyout-trigger]`).first();
      if (await declencheur.count()) {
        await page.keyboard.press('Shift');
        await declencheur.focus();
        await page.keyboard.press('Enter');
        const panneau = page.locator('.flyout-panel').first();
        try {
          await panneau.waitFor({ timeout: 4000 });
        } catch {
          defauts.push('clavier : Entrée n\'ouvre pas le menu déroulant (2.1.1)');
        }
        if (await panneau.count()) {
          await page.waitForTimeout(250);
          defauts.push(...(await mesurerContraste(page, '.flyout-panel', 'menu déroulant')));
          defauts.push(...(await tailleDesCibles(page, '.flyout-panel', 'menu déroulant')));
          const ligne = page.locator('.flyout-panel .flyout-link').nth(1);
          if (await ligne.count()) {
            await ligne.hover();
            await page.waitForTimeout(80);
            defauts.push(...(await mesurerContraste(page, '.flyout-panel', 'menu déroulant, ligne survolée')));
            const f = await mesurerFocus(page, '.flyout-panel .flyout-link >> nth=1', 'ligne du menu déroulant');
            if (f) {
              defauts.push(f);
            }
          }
          defauts.push(...(await defautsAxe(page, [racine, '.flyout-panel'], 'axe, menu ouvert')));
          await page.keyboard.press('Escape');
          await page.waitForTimeout(250);
          if (await page.locator('.flyout-panel').count()) {
            defauts.push('clavier : Échap ne ferme pas le menu déroulant (2.1.2)');
          } else {
            const rendu = await page.evaluate(() => (document.activeElement as HTMLElement | null)?.closest('[data-flyout-trigger]') !== null);
            if (!rendu) {
              defauts.push('clavier : le focus n\'est pas rendu au déclencheur après Échap (2.4.3)');
            }
          }
        }
      }

      // — règles WCAG 2 A/AA d'axe sur les barres, menu fermé
      defauts.push(...(await defautsAxe(page, [racine], 'axe, repos')));

      const cle = disposition;
      if (theme === 'actuel') {
        reference.set(cle, new Set(defauts));
      }
      const connus = reference.get(cle) ?? new Set<string>();
      const nouveaux = theme === 'actuel' ? [] : defauts.filter(d => !connus.has(d));
      mkdirSync(DOSSIER, { recursive: true });
      writeFileSync(join(DOSSIER, `menus-${disposition}-${theme}.json`), JSON.stringify({ total: defauts.length, nouveaux, defauts }, null, 2));
      console.log(`menus ${disposition} — ${theme} : ${defauts.length} défaut(s) dont ${nouveaux.length} propre(s) au thème`);
      expect(nouveaux, `défauts propres au thème « ${theme} » (${disposition})`).toEqual([]);
    });
  }
}
